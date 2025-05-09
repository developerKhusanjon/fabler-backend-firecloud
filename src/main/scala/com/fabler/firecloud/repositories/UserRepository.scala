package com.fabler.firecloud.repositories

import cats.effect.IO
import com.fabler.firecloud.models.User
import com.fabler.firecloud.database.FirebaseClient
import org.typelevel.log4cats.Logger

trait UserRepository {
  def create(user: User): IO[User]
  def findById(id: String): IO[Option[User]]
  def findByEmail(email: String): IO[Option[User]]
  def update(user: User): IO[User]
  def delete(id: String): IO[Boolean]
}

class UserRepositoryImpl(
                          private val firebaseClient: FirebaseClient
                        )(implicit val logger: Logger[IO]) extends UserRepository {

  private val collectionName = "users"

  override def create(user: User): IO[User] = {
    for {
      _ <- logger.debug(s"Creating user in repository: ${user.id}")
      result <- firebaseClient.create(collectionName, user.id, user)
    } yield result
  }

  override def findById(id: String): IO[Option[User]] = {
    for {
      _ <- logger.debug(s"Finding user by id: $id")
      userOpt <- firebaseClient.getById[User](collectionName, id)
    } yield userOpt
  }

  override def findByEmail(email: String): IO[Option[User]] = {
    for {
      _ <- logger.debug(s"Finding user by email: $email")
      users <- firebaseClient.query[User](
        collectionName,
        field = "email",
        operator = "==",
        value = email
      )
    } yield users.headOption
  }

  override def update(user: User): IO[User] = {
    for {
      _ <- logger.debug(s"Updating user: ${user.id}")
      updated <- firebaseClient.update(collectionName, user.id, user)
    } yield updated
  }

  override def delete(id: String): IO[Boolean] = {
    for {
      _ <- logger.debug(s"Deleting user: $id")
      deleted <- firebaseClient.delete(collectionName, id)
    } yield deleted
  }
}