package com.fabler.firecloud.models

import java.time.Instant
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._

case class User(
                 id: String,
                 email: String,
                 displayName: String,
                 photoUrl: Option[String] = None,
                 createdAt: Instant = Instant.now(),
                 updatedAt: Instant = Instant.now(),
                 lastLoginAt: Instant = Instant.now(),
                 preferences: UserPreferences = UserPreferences()
               )

object User {
  implicit val decoder: Decoder[User] = deriveDecoder[User]
  implicit val encoder: Encoder[User] = deriveEncoder[User]
}

case class UserPreferences(
                            theme: String = "light",
                            notificationsEnabled: Boolean = true,
                            audioQuality: String = "high"
                          )

object UserPreferences {
  implicit val decoder: Decoder[UserPreferences] = deriveDecoder[UserPreferences]
  implicit val encoder: Encoder[UserPreferences] = deriveEncoder[UserPreferences]
}

case class AuthToken(token: String)

object AuthToken {
  implicit val decoder: Decoder[AuthToken] = deriveDecoder[AuthToken]
  implicit val encoder: Encoder[AuthToken] = deriveEncoder[AuthToken]
}
