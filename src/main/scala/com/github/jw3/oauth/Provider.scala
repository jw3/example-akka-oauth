package com.github.jw3.oauth

import java.util.{Date, UUID}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.MessageEntity
import akka.stream.ActorMaterializer
import com.github.jw3.oauth.models.User
import com.github.jw3.oauth.protocols._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalaoauth2.provider
import scalaoauth2.provider.{AccessToken, AuthInfo, _}


trait Provider extends OAuth2Provider[User] {

  def routes()(implicit sys: ActorSystem, mat: ActorMaterializer) =
    accessTokenRoute ~
      post {
        path("auth") {
          authenticateOAuth2Async("com.github.jw3.oauth", oauth2Authenticator) { info =>
            complete(Marshal(info).to[MessageEntity])
          }
        }
      }
}

object Provider extends Provider {
  val oauth2DataHandler: DataHandler[User] = ExampleOAuthDataHandler

  val tokenEndpoint: TokenEndpoint = new TokenEndpoint {
    override val handlers = Map(
      provider.OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials
    )
  }
}


object ExampleOAuthDataHandler extends DataHandler[User] {

  val users = Seq(User(1, "foo"))
  val tokens = mutable.Map[User, AccessToken]()
  val info = mutable.Map[AuthInfo[User], String]()
  val rinfo = mutable.Map[String, AuthInfo[User]]()


  override def findUser(request: AuthorizationRequest): Future[Option[User]] = {
    Future.successful(users.map(u => u.name -> u).toMap.get(request.param("client_id").get))
  }

  override def createAccessToken(authInfo: AuthInfo[User]): Future[AccessToken] = {
    val tokenstr = UUID.randomUUID.toString.take(7)
    val token = AccessToken(tokenstr, None, None, Some(0L), new Date())
    tokens(authInfo.user) = token
    info(authInfo) = tokenstr
    rinfo(tokenstr) = authInfo
    Future.successful(token)
  }

  override def findAccessToken(token: String): Future[Option[AccessToken]] = {
    Future.successful(tokens.collectFirst { case t if t._2.token == token => t._2 })
  }

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] = {
    Future.successful(rinfo.get(accessToken.token))
  }

  override def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] = {
    Future.successful(tokens.get(authInfo.user))
  }

  override def validateClient(request: AuthorizationRequest): Future[Boolean] = Future.successful(true)
  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = Future.successful(None)
  override def deleteAuthCode(code: String): Future[Unit] = Future.successful(Unit)
  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] = Future.successful(None)
  override def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] = Future.successful(AccessToken("", Some(""), Some(""), Some(0L), new Date()))
}


object BootProvider extends App {
  implicit val sys = ActorSystem("oauth-provider")
  implicit val mat = ActorMaterializer()

  Http().bindAndHandle(Provider.routes(), "0.0.0.0", 8080)
}
