package it.pagopa.interop

import akka.NotUsed
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.pagopa.interop.tenantmanagement.api.impl._
import it.pagopa.interop.tenantmanagement.model._
import org.scalamock.scalatest.MockFactory

import java.net.InetAddress
import java.time.{OffsetDateTime, ZoneOffset}

package object tenantmanagement extends MockFactory {

  final lazy val url: String                =
    s"http://localhost:18088/tenant-management/${buildinfo.BuildInfo.interfaceVersion}"
  final val requestHeaders: Seq[HttpHeader] =
    Seq(
      headers.Authorization(OAuth2BearerToken("token")),
      headers.RawHeader("X-Correlation-Id", "test-id"),
      headers.`X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
    )

  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  val emptyData: Source[ByteString, NotUsed] = Source.empty[ByteString]

  implicit def toEntityMarshallerTokenSeed: ToEntityMarshaller[TenantSeed] =
    sprayJsonMarshaller[TenantSeed]

  implicit def fromEntityUnmarshallerTenant: FromEntityUnmarshaller[Tenant] =
    sprayJsonUnmarshaller[Tenant]

  implicit def fromEntityUnmarshallerProblem: FromEntityUnmarshaller[Problem] =
    sprayJsonUnmarshaller[Problem]

}
