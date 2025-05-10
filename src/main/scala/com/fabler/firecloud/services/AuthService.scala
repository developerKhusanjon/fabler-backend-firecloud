package com.fabler.firecloud.services

import cats.effect.IO
import com.fabler.firecloud.models.User
import com.fabler.firecloud.repositories.UserRepository
import com.fabler.firecloud.database.FirebaseClient
import org.typelevel.log4cats.Logger
import java.time.Instant

trait AuthService {
  def registerUser(user: User): IO[User]
  def authenticateToken(token: String): IO[Option[User]]
  def getUserById(id: String): IO[Option[User]]
}

class AuthServiceImpl(
                       private val userRepository: UserRepository,
                       private val firebaseClient: FirebaseClient
                     )(implicit val logger: Logger[IO]) extends AuthService {

  override def registerUser(user: User): IO[User] = {
    for {
      _ <- logger.info(s"Registering new user with email: ${user.email}")
      now = Instant.now()
      newUser = user.copy(
        createdAt = now,
        updatedAt = now
      )
      createdUser <- userRepository.create(newUser)
      _ <- logger.info(s"Created user with ID: ${createdUser.id}")
    } yield createdUser
  }

  override def authenticateToken(token: String): IO[Option[User]] = {
    for {
      _ <- logger.info("Authenticating token")
      userIdOpt <- firebaseClient.verifyIdToken(token)
      userOpt <- userIdOpt match {
        case Some(userId) =>
          logger.info(s"Token verification successful for user: $userId") *>
            userRepository.findById(userId)
        case None =>
          logger.warn("Token verification failed") *>
            IO.pure(None)
      }
    } yield userOpt
  }

  override def getUserById(id: String): IO[Option[User]] = {
    for {
      _ <- logger.info(s"Getting user by ID: $id")
      userOpt <- userRepository.findById(id)
    } yield userOpt
  }
}
