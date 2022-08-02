package it.pagopa.interop.tenantmanagement

import akka.actor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.pagopa.interop.tenantmanagement.model._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait SpecHelper {

  def createTenant(seed: TenantSeed)(implicit ec: ExecutionContext, actorSystem: actor.ActorSystem): Future[Tenant] =
    for {
      data   <- Marshal(seed).to[MessageEntity].map(_.dataBytes)
      tenant <- Unmarshal(makeRequest(data, "tenants", HttpMethods.POST)).to[Tenant]
    } yield tenant

  def getTenant(id: UUID)(implicit ec: ExecutionContext, actorSystem: actor.ActorSystem): Future[Tenant] = {
    val response = makeRequest(emptyData, s"tenants/${id.toString}", HttpMethods.GET)
    Unmarshal(response).to[Tenant]
  }

  def makeRequest(data: Source[ByteString, Any], path: String, verb: HttpMethod)(implicit
    actorSystem: ActorSystem
  ): HttpResponse = {
    Await.result(
      Http().singleRequest(
        HttpRequest(
          uri = s"$url/$path",
          method = verb,
          entity = HttpEntity(ContentTypes.`application/json`, data),
          headers = requestHeaders
        )
      ),
      Duration.Inf
    )
  }

  def makeFailingRequest[T](url: String, verb: HttpMethod, data: T)(implicit
    ec: ExecutionContext,
    actorSystem: actor.ActorSystem,
    marshaller: Marshaller[T, MessageEntity]
  ): Future[Problem] =
    for {
      body    <- Marshal(data).to[MessageEntity].map(_.dataBytes)
      problem <- Unmarshal(makeRequest(body, url, verb)).to[Problem]
    } yield problem

  def makeFailingRequest(url: String, verb: HttpMethod)(implicit
    ec: ExecutionContext,
    actorSystem: actor.ActorSystem
  ): Future[Problem] = makeFailingRequest(url, verb, "")
}
