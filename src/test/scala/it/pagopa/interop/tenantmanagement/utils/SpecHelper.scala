package it.pagopa.interop.tenantmanagement.utils

import cats.implicits._
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.pagopa.interop.tenantmanagement.model._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.model.headers._
import it.pagopa.interop.tenantmanagement.api.impl._

import java.net.InetAddress
import java.time.OffsetDateTime

trait SpecHelper {

  final lazy val url: String = s"http://localhost:18088/tenant-management/${buildinfo.BuildInfo.interfaceVersion}"

  final val requestHeaders: List[HttpHeader] = List(
    Authorization(OAuth2BearerToken("token")),
    RawHeader("X-Correlation-Id", "test-id"),
    `X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
  )

  def randomTenantAndSeed(offsetDateTime: OffsetDateTime): (Tenant, TenantSeed) = {
    val tenantId: UUID         = UUID.randomUUID()
    val externalId: ExternalId = ExternalId("IPA", "pippo")

    val attribute: TenantAttribute = TenantAttribute(
      id = UUID.randomUUID(),
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = OffsetDateTime.now()
    )

    val tenantSeed: TenantSeed = TenantSeed(
      id = tenantId.some,
      externalId = externalId,
      features = TenantFeature(Certifier("foo").some) :: Nil,
      attributes = attribute :: Nil
    )

    val tenant: Tenant = Tenant(
      id = tenantId,
      selfcareId = None,
      externalId = externalId,
      features = TenantFeature(Certifier("foo").some) :: Nil,
      attributes = attribute :: Nil,
      createdAt = offsetDateTime,
      updatedAt = None
    )

    (tenant, tenantSeed)
  }

  def createTenant(seed: TenantSeed)(implicit actorSystem: ActorSystem[_]): Future[Tenant] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      data          <- Marshal(seed).to[MessageEntity].map(_.dataBytes)
      responseBytes <- makeRequest(data, "tenants", HttpMethods.POST)
      tenant        <- Unmarshal(responseBytes).to[Tenant]
    } yield tenant
  }

  def getTenant(id: UUID)(implicit actorSystem: ActorSystem[_]): Future[Tenant] =
    get[Tenant](s"tenants/${id.toString}")

  private def get[T](
    path: String
  )(implicit actorSystem: ActorSystem[_], um: Unmarshaller[HttpResponse, T]): Future[T] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      responseBytes <- Http().singleRequest(
        HttpRequest(uri = s"$url/$path", method = HttpMethods.GET, headers = requestHeaders)
      )
      response      <- Unmarshal(responseBytes).to[T]
    } yield response
  }

  def makeRequest(data: Source[ByteString, Any], path: String, verb: HttpMethod)(implicit
    actorSystem: ActorSystem[_]
  ): Future[HttpResponse] = Http().singleRequest(
    HttpRequest(
      uri = s"$url/$path",
      method = verb,
      entity = HttpEntity(ContentTypes.`application/json`, data),
      headers = requestHeaders
    )
  )

  def makeFailingRequest[T](verb: HttpMethod, url: String, data: T)(implicit
    actorSystem: ActorSystem[_],
    marshaller: Marshaller[T, MessageEntity]
  ): Future[Problem] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      body          <- Marshal(data).to[MessageEntity].map(_.dataBytes)
      responseBytes <- makeRequest(body, url, verb)
      problem       <- Unmarshal(responseBytes).to[Problem]
    } yield problem
  }

  def makeFailingGet(url: String)(implicit actorSystem: ActorSystem[_]): Future[Problem] =
    makeFailingRequest(HttpMethods.GET, url, "")
}
