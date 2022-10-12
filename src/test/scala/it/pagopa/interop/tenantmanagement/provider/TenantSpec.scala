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
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ecs: ExecutionContext   = system.executionContext
    val (expected, tenantSeed)           = randomTenantAndSeed(mockedTime, mockedUUID)
    createTenant[Tenant](tenantSeed).map(tenant => assertEquals(tenant, expected))
  }

  test("Creation of a new tenant must fail if already exists") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext
    val (_, tenantSeed)                  = randomTenantAndSeed(mockedTime, mockedUUID)

    createTenant[Tenant](tenantSeed) >> createTenant[Problem](tenantSeed).map { result =>
      assertEquals(result.status, 409)
      assertEquals(result.errors.map(_.code), Seq("018-0001"))
    }
  }

  test("Retrieve of a tenant must succeed if tenant exists") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext
    val (expected, tenantSeed)           = randomTenantAndSeed(mockedTime, mockedUUID)

    val response: Future[Tenant] = createTenant[Tenant](tenantSeed) >> getTenant[Tenant](expected.id)
    response.map(tenant => assertEquals(tenant, expected))
  }

  test("Retrieve of a tenant must fail if tenant does not exist") {
    val (system, _, _)                = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    getTenant[Problem](UUID.randomUUID()).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Update of a tenant must fail if tenant does not exist") {
    val (system, _, _)                = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    val tenantDelta: TenantDelta = TenantDelta(None, List.empty)

    updateTenant[Problem](UUID.randomUUID(), tenantDelta).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Update of a tenant must succeed if tenant exists") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, mockedUUID)

    val tenantDelta: TenantDelta = TenantDelta(None, List.empty)

    createTenant(tenantSeed) >> updateTenant[Tenant](tenant.id, tenantDelta).map { result =>
      assertEquals(result, tenant.copy(selfcareId = None, features = Nil))
    }
  }

  test("Get of a tenant by externalId must succeed if tenant exist") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, mockedUUID)

    createTenant[Tenant](tenantSeed) >> getTenantByExternalId[Tenant](tenant.externalId.origin, tenant.externalId.value)
      .map { result =>
        assertEquals(result, tenant)
      }
  }

  test("Get of a tenant by externalId must fail if tenant does not exist") {
    val (system, _, _)                = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    getTenantByExternalId[Problem]("fakeOrigin", "fakeValue").map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Retrieving an attribute must fail if tenant does not exist") {
    val (system, _, _)                = suiteState()
    implicit val s: ActorSystem[_]    = system
    implicit val ec: ExecutionContext = system.executionContext

    getTenantAttribute[Problem](UUID.randomUUID().toString, UUID.randomUUID().toString).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Retrieving an attribute must fail if attribute in the tenant does not exist") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, mockedUUID)

    createTenant(tenantSeed) >> getTenantAttribute[Problem](tenant.id.toString, UUID.randomUUID.toString).map {
      result =>
        assertEquals(result.status, 404)
        assertEquals(result.errors.map(_.code), Seq("018-0007"))
    }
  }

  test("Retrieving an attribute must succeed if tenant and attribute exist") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, mockedUUID)
    val expected             = tenant.attributes.head

    createTenant(tenantSeed) >> getTenantAttribute[TenantAttribute](
      tenant.id.toString,
      tenant.attributes.head.certified.get.id.toString
    )
      .map { result => assertEquals(result, expected) }
  }

  test("Getting a tenant by selfcareId must succeed if a tenant has that selfcareId") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, mockedUUID)
    val selfcareId: String   = UUID.randomUUID().toString()
    val delta: TenantDelta   = TenantDelta(selfcareId.some, TenantFeature(Certifier("foo").some) :: Nil)

    createTenant[Tenant](tenantSeed) >> updateTenant[Tenant](tenant.id, delta) >> getTenantBySelfcareId[Tenant](
      selfcareId
    ).map { result =>
      assertEquals(result, tenant.copy(selfcareId = selfcareId.some))
    }
  }

  test("Getting a tenant by selfcareId must fail if no tenant has that selfcareId") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, mockedUUID)
    val selfcareId: String   = UUID.randomUUID().toString()
    val delta: TenantDelta   = TenantDelta(selfcareId.some, TenantFeature(Certifier("foo").some) :: Nil)

    createTenant[Tenant](tenantSeed) >> updateTenant[Tenant](tenant.id, delta) >> getTenantBySelfcareId[Problem](
      UUID.randomUUID().toString()
    ).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code).toList, List("018-0004"))
    }
  }

  test("Adding an attribute must fail if tenant does not exist") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val attr: TenantAttribute = attribute(mockedTime, mockedUUID)

    addTenantAttribute[Problem](UUID.randomUUID().toString, attr).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Adding an attribute must fail if attribute in the tenant already exists") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, mockedUUID)
    val attr: TenantAttribute = attribute(mockedTime, mockedUUID)

    createTenant(tenantSeed) >> addTenantAttribute[Problem](tenant.id.toString, attr).map { result =>
      assertEquals(result.status, 409)
      assertEquals(result.errors.map(_.code), Seq("018-0006"))
    }
  }

  test("Adding an attribute must succeed if tenant exists and attribute does not") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, mockedUUID)
    val attr: TenantAttribute = attribute(mockedTime, mockedUUID)

    val expected: Tenant = tenant.copy(attributes = attr :: Nil, updatedAt = mockedTime.some)

    createTenant(tenantSeed.copy(attributes = Nil)) >> addTenantAttribute[Tenant](tenant.id.toString, attr).map {
      result => assertEquals(result, expected)
    }
  }

  test("Updating an attribute must fail if tenant does not exist") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val attr: TenantAttribute = attribute(mockedTime, mockedUUID)

    updateTenantAttribute[Problem](UUID.randomUUID().toString, attr.certified.map(_.id.toString).get, attr).map {
      result =>
        assertEquals(result.status, 404)
        assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Updating an attribute must fail if attribute in the tenant doesn't exists") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, mockedUUID)
    val attrId                = UUID.randomUUID()
    val attr: TenantAttribute = {
      val attr = attribute(mockedTime, mockedUUID)
      attr.copy(certified = attr.certified.map(_.copy(id = attrId)))
    }

    createTenant(tenantSeed) >> updateTenantAttribute[Problem](tenant.id.toString, attrId.toString, attr)
      .map { result =>
        assertEquals(result.status, 404)
        assertEquals(result.errors.map(_.code), Seq("018-0007"))
      }
  }

  test("Updating an attribute must succeed if attribute exists in tenant") {
    val (system, mockedTime, mockedUUID) = suiteState()
    implicit val s: ActorSystem[_]       = system
    implicit val ec: ExecutionContext    = system.executionContext

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, mockedUUID)
    val attr: TenantAttribute = attribute(mockedTime, mockedUUID)

    val expected = tenant.copy(attributes = attr :: Nil, updatedAt = mockedTime.some)

    createTenant(tenantSeed) >> updateTenantAttribute[Tenant](
      tenant.id.toString,
      tenant.attributes.head.certified.get.id.toString,
      attr
    ).map { result => assertEquals(result, expected) }
  }

}
