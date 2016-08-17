package com.github.jw3.oauth

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.jw3.oauth.models.User
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


object models {
  case class User(id: Long, name: String)
}

object protocols extends DefaultJsonProtocol with SprayJsonSupport {
  import scalaoauth2.provider.AuthInfo

  implicit val UserFormat: RootJsonFormat[User] = jsonFormat2(User)
  implicit val AuthInfoFormat: RootJsonFormat[AuthInfo[User]] = jsonFormat4(AuthInfo[User])
}
