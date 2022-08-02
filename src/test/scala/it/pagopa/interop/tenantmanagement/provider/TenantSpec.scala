package it.pagopa.interop.tenantmanagement.provider

import akka.http.scaladsl.model.HttpMethods
import it.pagopa.interop.tenantmanagement._
import it.pagopa.interop.tenantmanagement.model._

import java.util.UUID
import scala.concurrent.Future

class TenantSpec extends BaseIntegrationSpec {

  "Creation of a new tenant" must {

    "succeed" in {
      val tenantId   = UUID.randomUUID()
      val attributes = TenantAttributes(certified = Seq.empty, declared = Seq.empty, verified = Seq.empty)

      val tenantSeed = TenantSeed(id = tenantId, attributes = attributes)

      val response: Future[Tenant] = createTenant(tenantSeed)

      val expected = Tenant(id = tenantId, certifier = false, attributes = attributes)

      response.futureValue shouldBe expected
    }

    "fail if already exists" in {
      val tenantId   = UUID.randomUUID()
      val attributes = TenantAttributes(certified = Seq.empty, declared = Seq.empty, verified = Seq.empty)

      val tenantSeed = TenantSeed(id = tenantId, attributes = attributes)

      val response: Future[Problem] = for {
        _    <- createTenant(tenantSeed)
        fail <- makeFailingRequest(s"tenants", HttpMethods.POST, tenantSeed)
      } yield fail

      val result = response.futureValue
      result.status shouldBe 409
      result.errors.map(_.code) shouldBe Seq("018-0001")
    }

  }

  "Retrieve of a tenant" must {

    "succeed if tenant exists" in {
      val tenantId   = UUID.randomUUID()
      val attributes = TenantAttributes(certified = Seq.empty, declared = Seq.empty, verified = Seq.empty)

      val tenantSeed = TenantSeed(id = tenantId, attributes = attributes)

      val response: Future[Tenant] =
        for {
          _      <- createTenant(tenantSeed)
          tenant <- getTenant(tenantId)
        } yield tenant

      val expected = Tenant(id = tenantId, certifier = false, attributes = attributes)

      response.futureValue shouldBe expected
    }

    "fail if tenant does not exist" in {
      val tenantId = UUID.randomUUID()

      val response: Future[Problem] = makeFailingRequest(s"tenants/${tenantId.toString}", HttpMethods.GET)

      val result = response.futureValue
      result.status shouldBe 404
      result.errors.map(_.code) shouldBe Seq("018-0003")
    }
  }

}
