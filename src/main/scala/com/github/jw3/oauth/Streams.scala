package com.github.jw3.oauth

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.Future


object streams {
  type Connection = Flow[HttpRequest, HttpResponse, _]
  type RequestBuilder = HttpRequest => Future[HttpRequest]
  val apiPrefix: String = ""


  def uripath(suffix: String) = s"$apiPrefix$suffix"


  /**
   * create a http connection to the host and port as a Flow[HttpRequest, HttpResponse, ]
   */
  def connection(host: String, port: Int = 8080)(implicit system: ActorSystem): Connection = {
    Http().outgoingConnection(host, port)
  }


  /**
   * Create a GET HttpRequest as a Source
   */
  def get(path: String)(implicit system: ActorSystem): Source[HttpRequest, NotUsed] = {
    request(path)(r => Future.successful(r.withMethod(HttpMethods.GET)))
  }


  /**
   * Create a POST HttpRequest as a Source
   */
  def post(path: String)(implicit system: ActorSystem): Source[HttpRequest, NotUsed] = {
    request(path)(r => Future.successful(r.withMethod(HttpMethods.POST)))
  }


  /**
   * Create a HttpRequest as a Source with access to modify its construction
   */
  def request(path: String)(builder: RequestBuilder)(implicit system: ActorSystem): Source[HttpRequest, NotUsed] = {
    Source.fromFuture(builder(HttpRequest(uri = uripath(path))))
  }


  /**
   * extract a [[HttpResponse]] from the Stream
   */
  def response[T](implicit system: ActorSystem, mat: ActorMaterializer, um: Unmarshaller[HttpResponse, T]): Flow[HttpResponse, Either[StatusCode, T], _] = {
    import system.dispatcher
    Flow[HttpResponse].mapAsync(1) {
      case r@HttpResponse(StatusCodes.OK, _, _, _) => Unmarshal(r).to[T].map(Right(_))
      case HttpResponse(c, _, _, _) => Future.successful(Left(c))
    }
  }
}
