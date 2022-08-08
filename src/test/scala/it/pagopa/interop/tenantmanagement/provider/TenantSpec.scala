package it.pagopa.interop.tenantmanagement.provider

import cats.implicits._
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpMethods._
import it.pagopa.interop.tenantmanagement.utils.BaseIntegrationSpec
import it.pagopa.interop.tenantmanagement.model._
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import it.pagopa.interop.tenantmanagement.api.impl._

class TenantSpec extends BaseIntegrationSpec {

  test("Creation of a new tenant must succeed") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ecs: ExecutionContext  = system.executionContext
    val (expected, tenantSeed)          = randomTenantAndSeed

    createTenant(tenantSeed).map(tenant => assertEquals(tenant, expected))
  }

  test("Creation of a new tenant must fail if already exists") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ec: ExecutionContext   = system.executionContext
    val (_, tenantSeed)                 = randomTenantAndSeed

    val response: Future[Problem] = createTenant(tenantSeed) >> makeFailingRequest(POST, "tenants", tenantSeed)
    response.map { result =>
      assertEquals(result.status, 409)
      assertEquals(result.errors.map(_.code), Seq("018-0001"))
    }
  }

  test("Retrieve of a tenant must succeed if tenant exists") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ec: ExecutionContext   = system.executionContext
    val (expected, tenantSeed)          = randomTenantAndSeed

    val response: Future[Tenant] = createTenant(tenantSeed) >> getTenant(expected.id)
    response.map(tenant => assertEquals(tenant, expected))
  }

  test("Retrieve of a tenant must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = actorSystem()
    implicit val ec: ExecutionContext   = system.executionContext

    makeFailingGet(s"tenants/${UUID.randomUUID().toString}").map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

}
