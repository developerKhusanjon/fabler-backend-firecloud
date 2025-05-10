package com.fabler.firecloud.database

import cats.effect.{IO, Resource}
import com.fabler.firecloud.config.FirebaseConfig
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.{DocumentReference, DocumentSnapshot, Firestore, QuerySnapshot}
import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.google.firebase.cloud.FirestoreClient
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import cats.implicits.*

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
      jsonData = data.asJson.noSpaces
      dataMap = decode[Map[String, Any]](jsonData).getOrElse(Map.empty)
      javaDataMap = dataMap.map {
        case (k, v) =>
          val convertedValue = v match {
            case m: Map[String, Any] @unchecked => m.asJava
            case other => other.asInstanceOf[AnyRef]
          }
          k -> convertedValue
      }.asJava
      _ <- IO(firestore.collection(collection).document(id).set(javaDataMap))
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
      jsonData = data.asJson.noSpaces
      dataMap = decode[Map[String, Any]](jsonData).getOrElse(Map.empty)
      javaDataMap = dataMap.map {
        case (k, v) =>
          val convertedValue = v match {
            case m: Map[String, Any] @unchecked => m.asJava
            case other => other.asInstanceOf[AnyRef]
          }
          k -> convertedValue
      }.asJava
      _ <- IO(firestore.collection(collection).document(id).set(javaDataMap))
    } yield data
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
