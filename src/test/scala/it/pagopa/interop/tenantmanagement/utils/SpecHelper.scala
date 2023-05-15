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
import akka.stream.Materializer

trait SpecHelper {

  final lazy val url: String = s"http://localhost:18088/tenant-management/${buildinfo.BuildInfo.interfaceVersion}"

  final val requestHeaders: List[HttpHeader] = List(
    Authorization(OAuth2BearerToken("token")),
    RawHeader("X-Correlation-Id", "test-id"),
    `X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
  )

  implicit val um: Unmarshaller[HttpResponse, Unit] = new Unmarshaller[HttpResponse, Unit] {
    override def apply(value: HttpResponse)(implicit ec: ExecutionContext, materializer: Materializer): Future[Unit] = {
      if (value.status.isSuccess) Future.unit else Future.failed(new Exception("Status != 2xx"))
    }
  }

  def attribute(offsetDateTime: OffsetDateTime, uuid: UUID): TenantAttribute =
    TenantAttribute(certified = CertifiedTenantAttribute(id = uuid, assignmentTimestamp = offsetDateTime).some)

  def randomTenantAndSeed(offsetDateTime: OffsetDateTime, uuid: UUID): (Tenant, TenantSeed) = {
    val tenantId: UUID         = UUID.randomUUID()
    val externalId: ExternalId = ExternalId(UUID.randomUUID().toString(), UUID.randomUUID().toString())

    val attr: TenantAttribute = attribute(offsetDateTime, uuid)

    val tenantSeed: TenantSeed = TenantSeed(
      id = tenantId.some,
      kind = TenantKind.PA,
      externalId = externalId,
      features = TenantFeature(Certifier("foo").some) :: Nil,
      attributes = attr :: Nil,
      name = "test_name"
    )

    val tenant: Tenant = Tenant(
      id = tenantId,
      kind = TenantKind.PA.some,
      selfcareId = None,
      externalId = externalId,
      features = TenantFeature(Certifier("foo").some) :: Nil,
      attributes = attr :: Nil,
      createdAt = offsetDateTime,
      updatedAt = None,
      mails = Nil,
      name = "test_name"
    )

    (tenant, tenantSeed)
  }

  def createTenant[T](
    seed: TenantSeed
  )(implicit actorSystem: ActorSystem[_], um: Unmarshaller[HttpResponse, T]): Future[T] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      data   <- Marshal(seed).to[MessageEntity].map(_.dataBytes)
      result <- performCall[T](HttpMethods.POST, s"tenants", data.some)
    } yield result
  }

  def getTenant[T](id: UUID)(implicit actorSystem: ActorSystem[_], um: Unmarshaller[HttpResponse, T]): Future[T] =
    performCall[T](HttpMethods.GET, s"tenants/${id.toString()}", None)

  def deleteTenant[T](id: UUID)(implicit actorSystem: ActorSystem[_], um: Unmarshaller[HttpResponse, T]): Future[T] =
    performCall[T](HttpMethods.DELETE, s"tenants/${id.toString()}", None)

  def getTenantByExternalId[T](origin: String, code: String)(implicit
    actorSystem: ActorSystem[_],
    um: Unmarshaller[HttpResponse, T]
  ): Future[T] =
    performCall[T](HttpMethods.GET, s"tenants/origin/$origin/code/$code", None)

  def getTenantBySelfcareId[T](
    selfcareId: String
  )(implicit actorSystem: ActorSystem[_], um: Unmarshaller[HttpResponse, T]): Future[T] =
    performCall[T](HttpMethods.GET, s"tenants/selfcareId/${selfcareId}", None)

  def updateTenant[T](id: UUID, tenantDelta: TenantDelta)(implicit
    actorSystem: ActorSystem[_],
    um: Unmarshaller[HttpResponse, T]
  ): Future[T] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      data   <- Marshal(tenantDelta).to[MessageEntity].map(_.dataBytes)
      result <- performCall[T](HttpMethods.POST, s"tenants/${id.toString()}", data.some)
    } yield result
  }

  def getTenantAttribute[T](tenantId: String, attributeId: String)(implicit
    actorSystem: ActorSystem[_],
    um: Unmarshaller[HttpResponse, T]
  ): Future[T] = performCall[T](HttpMethods.GET, s"tenants/$tenantId/attributes/$attributeId", None)

  def addTenantAttribute[T](tenantId: String, attributeSeed: TenantAttribute)(implicit
    actorSystem: ActorSystem[_],
    um: Unmarshaller[HttpResponse, T]
  ): Future[T] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      data   <- Marshal(attributeSeed).to[MessageEntity].map(_.dataBytes)
      result <- performCall[T](HttpMethods.POST, s"tenants/${tenantId}/attributes", data.some)
    } yield result
  }

  def updateTenantAttribute[T](tenantId: String, attributeId: String, attributeSeed: TenantAttribute)(implicit
    actorSystem: ActorSystem[_],
    um: Unmarshaller[HttpResponse, T]
  ): Future[T] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      data   <- Marshal(attributeSeed).to[MessageEntity].map(_.dataBytes)
      result <- performCall[T](HttpMethods.POST, s"tenants/${tenantId}/attributes/${attributeId}", data.some)
    } yield result
  }

  private def performCall[T](verb: HttpMethod, path: String, data: Option[Source[ByteString, Any]])(implicit
    actorSystem: ActorSystem[_],
    um: Unmarshaller[HttpResponse, T]
  ) = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    for {
      responseBytes <- request(verb, path, data)
      result        <- Unmarshal(responseBytes).to[T].recoverWith { e =>
        println(s"$verb -> $path")
        responseBytes.entity.dataBytes.map(_.decodeString("UTF-8")).runForeach(println) >> Future.failed(e)
      }
    } yield result
  }

  private def request(verb: HttpMethod, path: String, data: Option[Source[ByteString, Any]])(implicit
    actorSystem: ActorSystem[_]
  ): Future[HttpResponse] = {
    val request: HttpRequest = HttpRequest(uri = s"$url/$path", method = verb, headers = requestHeaders)
    val finalRequest         =
      data.fold(request)(data => request.withEntity(entity = HttpEntity(ContentTypes.`application/json`, data)))
    Http().singleRequest(finalRequest)
  }

}
