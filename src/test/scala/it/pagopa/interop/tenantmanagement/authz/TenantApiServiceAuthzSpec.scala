package it.pagopa.interop.tenantmanagement.authz

import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.Entity
import cats.implicits._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.api._
import it.pagopa.interop.tenantmanagement.api.impl.AttributesApiMarshallerImpl._
import it.pagopa.interop.tenantmanagement.api.impl.{AttributesApiServiceImpl, _}
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.tenantmanagement.model.persistence.Command
import it.pagopa.interop.tenantmanagement.server.impl.Main.tenantPersistenceEntity
import it.pagopa.interop.tenantmanagement.utils.ClusteredMUnitRouteTest

import java.time.OffsetDateTime
import java.util.UUID

class TenantApiServiceAuthzSpec extends ClusteredMUnitRouteTest {
  override val testPersistentEntity: Entity[Command, ShardingEnvelope[Command]] = tenantPersistenceEntity

  val offsetDateTimeSupplierStub = new OffsetDateTimeSupplier {
    def get: OffsetDateTime = OffsetDateTime.now()
  }

  val tenantService: TenantApiService =
    new TenantApiServiceImpl(testKit.system, testAkkaSharding, testPersistentEntity, offsetDateTimeSupplierStub)

  val attributesService: AttributesApiService =
    new AttributesApiServiceImpl(testKit.system, testAkkaSharding, testPersistentEntity, offsetDateTimeSupplierStub)

  val fakeSeed: TenantSeed = TenantSeed(
    id = UUID.randomUUID().some,
    externalId = ExternalId("IPA", "pippo"),
    features = Nil,
    attributes = TenantAttribute(certified =
      CertifiedTenantAttribute(id = UUID.randomUUID, assignmentTimestamp = OffsetDateTime.now()).some
    ) :: Nil
  )

  val fakeAttribute: TenantAttribute = TenantAttribute(certified =
    CertifiedTenantAttribute(id = UUID.randomUUID, assignmentTimestamp = OffsetDateTime.now()).some
  )

  test("Tenant api operation authorization spec should accept authorized roles for createTenant") {
    validateAuthorization("createTenant", { implicit c: Seq[(String, String)] => tenantService.createTenant(fakeSeed) })
  }

  test("Tenant api operation authorization spec should accept authorized roles for getTenant") {
    validateAuthorization("getTenant", { implicit c: Seq[(String, String)] => tenantService.getTenant("fakeSeed") })
  }

  test("Tenant api operation authorization spec should accept authorized roles for getTenantByExternalId") {
    validateAuthorization(
      "getTenantByExternalId",
      { implicit c: Seq[(String, String)] => tenantService.getTenantByExternalId("fakeOrigin", "fakeCode") }
    )
  }

  test("Tenant api operation authorization spec should accept authorized roles for updateTenant") {
    validateAuthorization(
      "updateTenant",
      { implicit c: Seq[(String, String)] => tenantService.updateTenant("tenantId", TenantDelta(None, List.empty)) }
    )
  }

  test("Tenant api operation authorization spec should accept authorized roles for getTenantBySelfcareId") {
    validateAuthorization(
      "getTenantBySelfcareId",
      { implicit c: Seq[(String, String)] => tenantService.getTenantBySelfcareId("selfcareId") }
    )
  }

  test("Attributes api operation authorization spec should accept authorized roles for addTenantAttribute") {
    validateAuthorization(
      "addTenantAttribute",
      { implicit c: Seq[(String, String)] => attributesService.addTenantAttribute("tenantId", fakeAttribute) }
    )
  }

  test("Attributes api operation authorization spec should accept authorized roles for deleteTenantAttribute") {
    validateAuthorization(
      "deleteTenantAttribute",
      { implicit c: Seq[(String, String)] => attributesService.deleteTenantAttribute("tenantId", "fakeAttributeId") }
    )
  }

  test("Attributes api operation authorization spec should accept authorized roles for updateTenantAttribute") {
    validateAuthorization(
      "updateTenantAttribute",
      { implicit c: Seq[(String, String)] =>
        attributesService.updateTenantAttribute("tenantId", "fakeAttributeId", fakeAttribute)
      }
    )
  }

  test("Attributes api operation authorization spec should accept authorized roles for getTenantAttribute") {
    validateAuthorization(
      "getTenantAttribute",
      { implicit c: Seq[(String, String)] =>
        attributesService.getTenantAttribute("tenantId", "attributeId")
      }
    )
  }

}
