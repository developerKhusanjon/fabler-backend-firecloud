package com.fabler.firecloud.database

import cats.effect.{IO, Resource}
import com.fabler.firecloud.config.FirebaseConfig
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.{DocumentReference, DocumentSnapshot, Firestore, QuerySnapshot}
import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.google.firebase.cloud.FirestoreClient
import io.circe.parser.decode
import io.circe.Json
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import cats.implicits.*
import io.circe.{Json, Encoder, Decoder}
import io.circe.parser.parse
import io.circe.Json
import java.{lang => jl, util => ju}
import cats.effect.unsafe.IORuntime
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import java.io.FileInputStream

trait FirebaseClient {
  def create[T: Encoder](collection: String, id: String, data: T): IO[T]
  def getById[T: Decoder](collection: String, id: String): IO[Option[T]]
  def update[T: Encoder](collection: String, id: String, data: T): IO[T]
  def delete(collection: String, id: String): IO[Boolean]
  def query[T: Decoder](collection: String, field: String, operator: String, value: Any): IO[List[T]]
  def verifyIdToken(token: String): IO[Option[String]]
}

class FirebaseClientImpl(
                          private val firestore: Firestore
                        )(implicit val ec: ExecutionContext, val logger: Logger[IO]) extends FirebaseClient {

  private def documentToObject[T: Decoder](document: DocumentSnapshot): IO[Option[T]] = {
    if (!document.exists()) {
      IO.pure(None)
    } else {
      val jsonStr = document.getData.asScala.map { case (k, v) =>
        k -> (v match {
          case s: String => s""""$s""""
          case n: Number => n.toString
          case b: AnyRef => b.toString
          case null => "null"
          case _ => s""""${v.toString}""""
        })
      }.map { case (k, v) => s""""$k": $v""" }.mkString("{", ", ", "}")

      decode[T](jsonStr) match {
        case Right(obj) => IO.pure(Some(obj))
        case Left(error) =>
          logger.error(s"Error decoding Firestore document: ${error.getMessage}") *> IO.pure(None)
      }
    }
  }

  override def create[T: Encoder](collection: String, id: String, data: T): IO[T] = {
    for {
      _ <- logger.debug(s"Creating document in $collection with ID: $id")
      json = data.asJson
      javaMap <- convertJsonToJavaMap(json)
      _ <- IO(firestore.collection(collection).document(id).set(javaMap))
    } yield data
  }

  override def getById[T: Decoder](collection: String, id: String): IO[Option[T]] = {
    for {
      _ <- logger.debug(s"Getting document from $collection with ID: $id")
      docRef = firestore.collection(collection).document(id)
      docSnapshot <- IO(docRef.get())
      result <- documentToObject[T](docSnapshot.get())
    } yield result
  }

  override def update[T: Encoder](collection: String, id: String, data: T): IO[T] = {
    for {
      _ <- logger.debug(s"Updating document in $collection with ID: $id")
      json = data.asJson
      javaMap <- convertJsonToJavaMap(json)
      _ <- IO(firestore.collection(collection).document(id).set(javaMap))
    } yield data
  }

  private def convertJsonValue(json: Json): IO[AnyRef] = IO {
    json.fold[AnyRef](
      jsonNull = null,
      jsonBoolean = b => jl.Boolean.valueOf(b),
      jsonNumber = n => n.toBigDecimal match {
        case Some(bd) => bd.bigDecimal
        case None => jl.Double.valueOf(n.toDouble)
      },
      jsonString = s => s,
      jsonArray = arr => arr.map(convertJsonValueAndRun).asJava,
      jsonObject = obj => convertJsonToJavaMap(Json.fromJsonObject(obj)).unsafeRunSync()
    )
  }

  // New helper method to execute IO conversion
  private def convertJsonValueAndRun(json: Json): AnyRef =
    convertJsonValue(json).unsafeRunSync()
  
  private def convertJsonToJavaMap(json: Json): IO[ju.Map[String, AnyRef]] =
    json.asObject match {
      case Some(obj) =>
        obj.toMap.toList
          .traverse { case (k, v) =>
            convertJsonValue(v).map(k -> _)
          }
          .map(_.toMap.asJava)

      case None =>
        IO.raiseError(new IllegalArgumentException("JSON value must be an object"))
    }

  override def delete(collection: String, id: String): IO[Boolean] = {
    for {
      _ <- logger.debug(s"Deleting document from $collection with ID: $id")
      _ <- IO(firestore.collection(collection).document(id).delete())
    } yield true
  }

  override def query[T: Decoder](collection: String, field: String, operator: String, value: Any): IO[List[T]] = {
    for {
      _ <- logger.debug(s"Querying $collection where $field $operator $value")
      query = firestore.collection(collection).whereEqualTo(field, value)
      querySnapshot <- IO(query.get())
      documents = querySnapshot.get().getDocuments.asScala.toList
      results <- documents.map { doc =>
        documentToObject[T](doc)
      }.sequence
    } yield results.flatten
  }

  override def verifyIdToken(token: String): IO[Option[String]] = {
    // This would be implemented using Firebase Auth Admin SDK
    // For now, we'll return a placeholder implementation
    IO.pure(Some("user-id"))
  }
}

object FirebaseClient {
  def initialize(config: FirebaseConfig)(implicit ec: ExecutionContext, logger: Logger[IO]): IO[FirebaseClient] = {
    for {
      _ <- logger.info("Initializing Firebase client")
      credentials <- IO {
        val stream = new FileInputStream(config.credentialsFile)
        GoogleCredentials.fromStream(stream)
      }
      options <- IO {
        new FirebaseOptions.Builder()
          .setCredentials(credentials)
          .setDatabaseUrl(config.databaseUrl)
          .build()
      }
      _ <- IO {
        if (FirebaseApp.getApps.isEmpty) {
          FirebaseApp.initializeApp(options)
        }
      }
      firestore <- IO(FirestoreClient.getFirestore())
    } yield new FirebaseClientImpl(firestore)
  }
}
