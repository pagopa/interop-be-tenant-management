package it.pagopa.interop.tenantmanagement.provider

import cats.implicits._
import akka.actor.typed.ActorSystem
import it.pagopa.interop.tenantmanagement.utils.BaseIntegrationSpec
import it.pagopa.interop.tenantmanagement.model._
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import it.pagopa.interop.tenantmanagement.api.impl._
import cats.data.NonEmptyList
import java.time.OffsetDateTime

class TenantSpec extends BaseIntegrationSpec {

  test("Creation of a new tenant must succeed") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val (expected, tenantSeed)          = randomTenantAndSeed(mockedTime, UUID.randomUUID())
    createTenant[Tenant](tenantSeed).map(tenant => assertEquals(tenant, expected))
  }

  test("Creation of a new tenant must fail if already exists") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val (_, tenantSeed)                 = randomTenantAndSeed(mockedTime, UUID.randomUUID())

    createTenant[Tenant](tenantSeed) >> createTenant[Problem](tenantSeed).map { result =>
      assertEquals(result.status, 409)
      assertEquals(result.errors.map(_.code), Seq("018-0001"))
    }
  }

  test("Retrieve of a tenant must succeed if tenant exists") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val (expected, tenantSeed)          = randomTenantAndSeed(mockedTime, UUID.randomUUID())

    val response: Future[Tenant] = createTenant[Tenant](tenantSeed) >> getTenant[Tenant](expected.id)
    response.map(tenant => assertEquals(tenant, expected))
  }

  test("Retrieve of a tenant must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    getTenant[Problem](UUID.randomUUID()).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Update of a tenant must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val tenantDelta: TenantDelta = TenantDelta(None, Nil, Nil)

    updateTenant[Problem](UUID.randomUUID(), tenantDelta).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Update of a tenant must succeed if tenant exists") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())

    val tenantDelta: TenantDelta = TenantDelta(None, Nil, Nil)

    createTenant(tenantSeed) >> updateTenant[Tenant](tenant.id, tenantDelta).map { result =>
      assertEquals(result, tenant.copy(selfcareId = None, features = Nil))
    }
  }

  test("Update of a tenant with mail must succeed if tenant exists") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val mailseed: List[MailSeed] =
      List(
        MailSeed(MailKind.CONTACT_EMAIL, "foo@bar.it"),
        MailSeed(MailKind.CONTACT_EMAIL, "luke@theforce.com", "use the force".some)
      )

    val mails: List[Mail] = List(
      Mail(MailKind.CONTACT_EMAIL, "foo@bar.it", mockedTime),
      Mail(MailKind.CONTACT_EMAIL, "luke@theforce.com", mockedTime, "use the force".some)
    )

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())
    val expected: Tenant     = tenant.copy(selfcareId = None, features = Nil, mails = mails)

    val tenantDelta: TenantDelta = TenantDelta(None, Nil, mailseed)

    createTenant(tenantSeed) >> updateTenant[Tenant](tenant.id, tenantDelta).map { result =>
      assertEquals(result, expected)
    }
  }

  test("Update of a tenant with mail must not override existing mail createdAt field") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val time1: OffsetDateTime = OffsetDateTime.now()
    val time2: OffsetDateTime = time1.plusMinutes(10L)

    val mailseed1: List[MailSeed] =
      List(MailSeed(MailKind.CONTACT_EMAIL, "foo@bar.it"))

    val tenantDelta1: TenantDelta = TenantDelta(None, Nil, mailseed1)

    val mailseed2: List[MailSeed] = List(
      MailSeed(MailKind.CONTACT_EMAIL, "foo@bar.it", "awe".some),
      MailSeed(MailKind.CONTACT_EMAIL, "luke@theforce.com")
    )

    val tenantDelta2: TenantDelta = TenantDelta(None, Nil, mailseed2)

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())

    val expected1: Tenant =
      tenant.copy(selfcareId = None, features = Nil, mails = Mail(MailKind.CONTACT_EMAIL, "foo@bar.it", time1) :: Nil)

    val expected2: Tenant = tenant.copy(
      selfcareId = None,
      features = Nil,
      mails = List(
        Mail(MailKind.CONTACT_EMAIL, "foo@bar.it", time1, "awe".some),
        Mail(MailKind.CONTACT_EMAIL, "luke@theforce.com", time2)
      )
    )

    createTenant(tenantSeed) >>
      Future { mockedTimes = NonEmptyList.of(time1) } >>
      updateTenant[Tenant](tenant.id, tenantDelta1).map { assertEquals(_, expected1, "first update failed") } >>
      Future { mockedTimes = NonEmptyList.of(time2) } >>
      updateTenant[Tenant](tenant.id, tenantDelta2).map { assertEquals(_, expected2, "second update failed") }
  }

  test("Get of a tenant by externalId must succeed if tenant exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())

    createTenant[Tenant](tenantSeed) >> getTenantByExternalId[Tenant](tenant.externalId.origin, tenant.externalId.value)
      .map { result =>
        assertEquals(result, tenant)
      }
  }

  test("Get of a tenant by externalId must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    getTenantByExternalId[Problem]("fakeOrigin", "fakeValue").map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0008"))
    }
  }

  test("Retrieving an attribute must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    getTenantAttribute[Problem](UUID.randomUUID().toString, UUID.randomUUID().toString).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Retrieving an attribute must fail if attribute in the tenant does not exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())

    createTenant(tenantSeed) >> getTenantAttribute[Problem](tenant.id.toString, UUID.randomUUID.toString).map {
      result =>
        assertEquals(result.status, 404)
        assertEquals(result.errors.map(_.code), Seq("018-0007"))
    }
  }

  test("Retrieving an attribute must succeed if tenant and attribute exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())
    val expected             = tenant.attributes.head

    createTenant(tenantSeed) >> getTenantAttribute[TenantAttribute](
      tenant.id.toString,
      tenant.attributes.head.certified.get.id.toString
    )
      .map { result => assertEquals(result, expected) }
  }

  test("Getting a tenant by selfcareId must succeed if a tenant has that selfcareId") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())
    val selfcareId: String   = UUID.randomUUID().toString()
    val delta: TenantDelta   = TenantDelta(selfcareId.some, TenantFeature(Certifier("foo").some) :: Nil, Nil)

    createTenant[Tenant](tenantSeed) >> updateTenant[Tenant](tenant.id, delta) >> getTenantBySelfcareId[Tenant](
      selfcareId
    ).map { result =>
      assertEquals(result, tenant.copy(selfcareId = selfcareId.some))
    }
  }

  test("Getting a tenant by selfcareId must fail if no tenant has that selfcareId") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val (tenant, tenantSeed) = randomTenantAndSeed(mockedTime, UUID.randomUUID())
    val selfcareId: String   = UUID.randomUUID().toString()
    val delta: TenantDelta   = TenantDelta(selfcareId.some, TenantFeature(Certifier("foo").some) :: Nil, Nil)

    createTenant[Tenant](tenantSeed) >> updateTenant[Tenant](tenant.id, delta) >> getTenantBySelfcareId[Problem](
      UUID.randomUUID().toString()
    ).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code).toList, List("018-0010"))
    }
  }

  test("Adding an attribute must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val uuid: UUID                      = UUID.randomUUID()

    val attr: TenantAttribute = attribute(mockedTime, uuid)

    addTenantAttribute[Problem](uuid.toString, attr).map { result =>
      assertEquals(result.status, 404)
      assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Adding an attribute must fail if attribute in the tenant already exists") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val uuid: UUID                      = UUID.randomUUID()

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, uuid)
    val attr: TenantAttribute = attribute(mockedTime, uuid)

    createTenant(tenantSeed) >> addTenantAttribute[Problem](tenant.id.toString, attr).map { result =>
      assertEquals(result.status, 409)
      assertEquals(result.errors.map(_.code), Seq("018-0006"))
    }
  }

  test("Adding an attribute must succeed if tenant exists and attribute does not") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, UUID.randomUUID())
    val attr: TenantAttribute = attribute(mockedTime, UUID.randomUUID())

    val expected: Tenant = tenant.copy(attributes = attr :: Nil, updatedAt = mockedTime.some)

    createTenant(tenantSeed.copy(attributes = Nil)) >> addTenantAttribute[Tenant](tenant.id.toString, attr).map {
      result => assertEquals(result, expected)
    }
  }

  test("Updating an attribute must fail if tenant does not exist") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val attr: TenantAttribute           = attribute(mockedTime, UUID.randomUUID())

    updateTenantAttribute[Problem](UUID.randomUUID().toString, attr.certified.map(_.id.toString).get, attr).map {
      result =>
        assertEquals(result.status, 404)
        assertEquals(result.errors.map(_.code), Seq("018-0004"))
    }
  }

  test("Updating an attribute must fail if attribute in the tenant doesn't exists") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val uuid: UUID                      = UUID.randomUUID()

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, uuid)
    val attrId                = UUID.randomUUID()
    val attr: TenantAttribute = {
      val attr = attribute(mockedTime, uuid)
      attr.copy(certified = attr.certified.map(_.copy(id = attrId)))
    }

    createTenant(tenantSeed) >> updateTenantAttribute[Problem](tenant.id.toString, attrId.toString, attr)
      .map { result =>
        assertEquals(result.status, 404)
        assertEquals(result.errors.map(_.code), Seq("018-0007"))
      }
  }

  test("Updating an attribute must succeed if attribute exists in tenant") {
    implicit val system: ActorSystem[_] = suiteState()
    implicit val ecs: ExecutionContext  = system.executionContext
    val uuid: UUID                      = UUID.randomUUID()

    val (tenant, tenantSeed)  = randomTenantAndSeed(mockedTime, uuid)
    val attr: TenantAttribute = attribute(mockedTime, uuid)

    val expected = tenant.copy(attributes = attr :: Nil, updatedAt = mockedTime.some)

    createTenant(tenantSeed) >> updateTenantAttribute[Tenant](
      tenant.id.toString,
      tenant.attributes.head.certified.get.id.toString,
      attr
    ).map { result => assertEquals(result, expected) }
  }

}
