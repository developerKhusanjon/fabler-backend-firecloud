package com.fabler.firecloud.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.Monad
import cats.implicits._
import com.fabler.firecloud.models.User
import com.fabler.firecloud.services.AuthService
import org.http4s.{AuthedRoutes, ContextRequest, Request, Response, Status}
import org.http4s.server.AuthMiddleware
import org.http4s.headers.Authorization
import org.typelevel.log4cats.Logger

object Auth {
  def apply(authService: AuthService)(implicit logger: Logger[IO]): Auth = new Auth(authService)
}

class Auth(authService: AuthService)(implicit logger: Logger[IO]) {

  // Remove custom type aliases and case class
  implicit val ioMonad: Monad[IO] = Monad[IO]

  def withUser(req: Request[IO])(f: User => IO[Response[IO]]): IO[Response[IO]] = {
    extractBearerToken(req).flatMap {
      case Some(token) =>
        authService.authenticateToken(token).flatMap {
          case Some(user) => f(user)
          case None =>
            logger.warn(s"Authentication failed: Invalid token") *>
              IO.pure(Response[IO](Status.Unauthorized))
        }
      case None =>
        logger.warn("Authentication failed: No bearer token provided") *>
          IO.pure(Response[IO](Status.Unauthorized))
    }
  }

  private def extractBearerToken(req: Request[IO]): IO[Option[String]] = {
    req.headers.get[Authorization] match {
      case Some(authHeader) =>
        for {
          _ <- logger.debug("Processing authorization header")
          token <- IO(authHeader.credentials.renderString match {
            case s"Bearer $t" => Some(t)
            case _ => None
          })
          _ <- logger.debug(s"Token extraction result: ${token.isDefined}")
        } yield token

      case None =>
        logger.debug("No authorization header found") *> IO.pure(None)
    }
  }

  private val authUser: Kleisli[IO, Request[IO], Either[String, User]] =
    Kleisli { req =>
      extractBearerToken(req).flatMap {
        case Some(token) =>
          authService.authenticateToken(token).map {
            case Some(user) => Right(user)
            case None => Left("Invalid authentication token")
          }
        case None =>
          IO.pure(Left("Missing authentication token"))
      }
    }

  private val onFailure: AuthedRoutes[String, IO] = Kleisli {
    (req: ContextRequest[IO, String]) =>
      OptionT.liftF[IO, Response[IO]] {
        IO.pure(Response[IO](Status.Unauthorized).withEntity(req.context))
      }
  }

  val middleware: AuthMiddleware[IO, User] = AuthMiddleware(
    authUser,
    onFailure
  )
}