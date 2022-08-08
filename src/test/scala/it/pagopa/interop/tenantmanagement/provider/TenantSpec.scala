package it.pagopa.interop.tenantmanagement.provider

import cats.implicits._
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpMethods._
import it.pagopa.interop.tenantmanagement._
import it.pagopa.interop.tenantmanagement.model._
import java.util.UUID
import scala.concurrent.Future
import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import it.pagopa.interop.tenantmanagement.api.impl._

class TenantSpec extends BaseIntegrationSpec {

  test("Creation of a new tenant must succeed") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ecs: ExecutionContext  = system.executionContext
    val tenantId: UUID                  = UUID.randomUUID()
    val selfcareId: UUID                = UUID.randomUUID()
    val externalId: ExternalId          = ExternalId("IPA", "pippo")

    val attribute: TenantAttribute = TenantAttribute(
      id = UUID.randomUUID(),
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = OffsetDateTime.now()
    )

    val tenantSeed: TenantSeed = TenantSeed(
      id = tenantId.some,
      selfcareId = selfcareId,
      externalId = externalId,
      kind = true,
      attributes = attribute :: Nil
    )

    val response: Future[Tenant] = createTenant(tenantSeed)

    val expected: Tenant = Tenant(
      id = tenantId,
      selfcareId = selfcareId.toString,
      externalId = externalId,
      kind = true,
      attributes = attribute :: Nil
    )

    response.map(result => assertEquals(result, expected))
  }

  test("Creation of a new tenant must fail if already exists") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ec: ExecutionContext   = system.executionContext
    val tenantId: UUID                  = UUID.randomUUID()
    val selfcareId: UUID                = UUID.randomUUID()
    val externalId: ExternalId          = ExternalId("IPA", "pippo")

    val attribute: TenantAttribute = TenantAttribute(
      id = UUID.randomUUID(),
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = OffsetDateTime.now()
    )

    val tenantSeed: TenantSeed = TenantSeed(
      id = tenantId.some,
      selfcareId = selfcareId,
      externalId = externalId,
      kind = true,
      attributes = attribute :: Nil
    )

    val response: Future[Problem] =
      createTenant(tenantSeed) >> makeFailingRequest(POST, "tenants", tenantSeed)

    response.map { result =>
      assertEquals(result.status, 409)
      assertEquals(result.errors.map(_.code), Seq("018-0001"))
    }
  }

  test("Retrieve of a tenant must succeed if tenant exists") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ec: ExecutionContext   = system.executionContext
    val tenantId: UUID                  = UUID.randomUUID()
    val selfcareId: UUID                = UUID.randomUUID()
    val externalId: ExternalId          = ExternalId("IPA", "pippo")

    val attribute: TenantAttribute = TenantAttribute(
      id = UUID.randomUUID(),
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = OffsetDateTime.now()
    )

    val tenantSeed: TenantSeed = TenantSeed(
      id = tenantId.some,
      selfcareId = selfcareId,
      externalId = externalId,
      kind = true,
      attributes = attribute :: Nil
    )

    val response: Future[Tenant] = createTenant(tenantSeed) >> getTenant(tenantId)

    val expected: Tenant = Tenant(
      id = tenantId,
      selfcareId = selfcareId.toString,
      externalId = externalId,
      kind = true,
      attributes = attribute :: Nil
    )

    response.map(result => assertEquals(result, expected))
  }

  test("Retrieve of a tenant must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ec: ExecutionContext   = system.executionContext
    val tenantId: UUID                  = UUID.randomUUID()
    val response: Future[Problem]       = makeFailingGet(s"tenants/${tenantId.toString}")

    response.map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

}
