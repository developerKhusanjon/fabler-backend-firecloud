package com.fabler.firecloud.middleware

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.http4s.{Request, Response, Status}
import org.http4s.dsl.io._
import org.http4s.server.middleware.{Logger => HttpLogger}
import org.http4s.headers.Authorization
import org.typelevel.log4cats.Logger
import com.fabler.firecloud.models.User
import com.fabler.firecloud.services.AuthService

case class Auth(authService: AuthService)(implicit logger: Logger[IO]) {

  val authUser: Kleisli[OptionT[IO, *], Request[IO], User] = Kleisli { req =>
    OptionT {
      req.headers.get(Authorization.name) match {
        case Some(header) =>
          // Extract token from Authorization header (Bearer token)
          val token = header.head.value.replaceAll("Bearer ", "")
          authService.authenticateToken(token)
        case None =>
          logger.warn("No Authorization header found").as(None)
      }
    }
  }

  val onFailure: AuthedRoutes[String, IO] = Kleisli { req =>
    OptionT.pure[IO](Response[IO](Status.Unauthorized))
  }

  def middleware: AuthMiddleware[IO, User] = AuthMiddleware(authUser, onFailure("Unauthorized"))

  def withUser[A](req: Request[IO])(f: User => IO[Response[IO]]): IO[Response[IO]] = {
    req.headers.get(Authorization.name) match {
      case Some(header) =>
        val token = header.toString.replaceAll("Bearer ", "")
        authService.authenticateToken(token).flatMap {
          case Some(user) => f(user)
          case None => Unauthorized("Invalid authentication token")
        }
      case None => Unauthorized("Authentication required")
    }
  }
}

// Define these types to make the Auth class compile
type AuthedRoutes[Auth, F[_]] = Kleisli[F, AuthedRequest[F, Auth], Response[F]]
case class AuthedRequest[F[_], T](context: T, req: Request[F])
object AuthMiddleware {
  def apply[F[_], Auth](
                         authUser: Kleisli[F, Request[F], Auth],
                         onFailure: AuthedRoutes[String, F]
                       ): AuthMiddleware[F, Auth] = ???
}
type AuthMiddleware[F[_], Auth] = Kleisli[F, Request[F], Response[F]] => Kleisli[F, Request[F], Response[F]]
