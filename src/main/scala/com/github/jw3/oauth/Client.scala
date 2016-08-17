package com.github.jw3.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.github.jw3.oauth.models.User
import com.github.jw3.oauth.streams.{Connection, response}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalaoauth2.provider.AuthInfo


trait Client {

  def routes(conn: Connection)(implicit sys: ActorSystem, mat: ActorMaterializer) =
    get {
      path("open") {
        complete(OK)
      } ~
      path("closed") {
        authenticateOAuth2Async("com.github.jw3.oauth", authenticator(conn)) { user =>
          complete(OK)
        }
      }
    }

  def authenticator(conn: Connection)(creds: Credentials)(implicit sys: ActorSystem, mat: ActorMaterializer): Future[Option[String]] = {
    import protocols._

    creds match {
      case p@Credentials.Provided(token) =>
        streams.post("/auth")
        .map(_.withHeaders(Authorization(OAuth2BearerToken(token))))
        .via(conn)
        .via(response[AuthInfo[User]])
        .map {
          case Right(info) => Some(info.user.name)
          case Left(code) => None
        }
        .runWith(Sink.head)

      case _ =>
        Future.successful(None)
    }
  }
}

object Client extends Client


object BootClient extends App {
  implicit val sys = ActorSystem("oauth-client")
  implicit val mat = ActorMaterializer()

  val conn = streams.connection("0.0.0.0", 8080)
  Http().bindAndHandle(Client.routes(conn), "0.0.0.0", 8081)
}
