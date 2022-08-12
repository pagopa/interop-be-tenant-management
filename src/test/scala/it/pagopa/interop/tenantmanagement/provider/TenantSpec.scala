package it.pagopa.interop.tenantmanagement.provider

import cats.implicits._
import akka.actor.typed.ActorSystem
import it.pagopa.interop.tenantmanagement.utils.BaseIntegrationSpec
import it.pagopa.interop.tenantmanagement.model._
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import it.pagopa.interop.tenantmanagement.api.impl._

class TenantSpec extends BaseIntegrationSpec {

  test("Creation of a new tenant must succeed") {
    val (system, mockedTime)           = suiteState()
    implicit val s: ActorSystem[_]     = system
    implicit val ecs: ExecutionContext = system.executionContext
    val (expected, tenantSeed)         = randomTenantAndSeed(mockedTime)

    createTenant[Tenant](tenantSeed).map(tenant => assertEquals(tenant, expected))
  }

  test("Creation of a new tenant must fail if already exists") {
    val (system, mockedTime)          = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext
    val (_, tenantSeed)               = randomTenantAndSeed(mockedTime)

    createTenant[Tenant](tenantSeed) >> createTenant[Problem](tenantSeed).map { result =>
      assertEquals(result.status, 409)
      assertEquals(result.errors.map(_.code), Seq("018-0001"))
    }
  }

  test("Retrieve of a tenant must succeed if tenant exists") {
    val (system, mockedTime)          = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext
    val (expected, tenantSeed)        = randomTenantAndSeed(mockedTime)

    val response: Future[Tenant] = createTenant[Tenant](tenantSeed) >> getTenant[Tenant](expected.id)
    response.map(tenant => assertEquals(tenant, expected))
  }

  test("Retrieve of a tenant must fail if tenant does not exist") {
    val (system, _)                   = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    getTenant[Problem](UUID.randomUUID()).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Update of a tenant must fail if tenant does not exist") {
    val (system, _)                   = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    val tenantDelta: TenantDelta = TenantDelta(None, Nil)

    updateTenant[Problem](UUID.randomUUID(), tenantDelta).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Update of a tenant must succeed if tenant exists") {
    val (system, mockedTime)          = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime)

    val tenantDelta: TenantDelta = TenantDelta(None, Nil)

    createTenant(tenantSeed) >> updateTenant[Tenant](tenant.id, tenantDelta).map { result =>
      assertEquals(result, tenant.copy(selfcareId = None, features = Nil))
    }
  }

  test("Get of a tenant by externalId must succeed if tenant exist") {
    val (system, mockedTime)          = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime)

    createTenant[Tenant](tenantSeed) >> getTenantByExternalId[Tenant](tenant.externalId.origin, tenant.externalId.value)
      .map { result =>
        assertEquals(result, tenant)
      }
  }

  test("Get of a tenant by externalId must fail if tenant does not exist") {
    val (system, _)                   = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    getTenantByExternalId[Problem]("fakeOrigin", "fakeValue").map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

}
