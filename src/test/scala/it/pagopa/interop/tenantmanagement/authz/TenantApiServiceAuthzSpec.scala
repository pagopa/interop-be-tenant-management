package it.pagopa.interop.tenantmanagement.authz

import cats.implicits._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.Entity
import it.pagopa.interop.tenantmanagement.api._
import it.pagopa.interop.tenantmanagement.api.impl._
import it.pagopa.interop.tenantmanagement.api.impl.AttributesApiMarshallerImpl._
import it.pagopa.interop.tenantmanagement.model.persistence.Command
import it.pagopa.interop.tenantmanagement.model.{TenantSeed}
import it.pagopa.interop.tenantmanagement.server.impl.Main.tenantPersistenceEntity
import it.pagopa.interop.tenantmanagement.utils.ClusteredMUnitRouteTest

import java.util.UUID
import it.pagopa.interop.tenantmanagement.model.ExternalId
import java.time.OffsetDateTime
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.api.impl.AttributesApiServiceImpl

class TenantApiServiceAuthzSpec extends ClusteredMUnitRouteTest {
  override val testPersistentEntity: Entity[Command, ShardingEnvelope[Command]] = tenantPersistenceEntity

  val iCallConstructorsBecauseOOPIsNeverDead = new OffsetDateTimeSupplier {
    def get: OffsetDateTime = OffsetDateTime.now()
  }

  val tenantService: TenantApiService = new TenantApiServiceImpl(
    testKit.system,
    testAkkaSharding,
    testPersistentEntity,
    iCallConstructorsBecauseOOPIsNeverDead
  )

  val attributesService: AttributesApiService =
    new AttributesApiServiceImpl(
      testKit.system,
      testAkkaSharding,
      testPersistentEntity,
      iCallConstructorsBecauseOOPIsNeverDead
    )

  val fakeSeed: TenantSeed = TenantSeed(
    id = UUID.randomUUID().some,
    externalId = ExternalId("IPA", "pippo"),
    features = Nil,
    attributes = TenantAttribute(
      id = UUID.randomUUID,
      kind = TenantAttributeKind.CERTIFIED,
      assignmentTimestamp = OffsetDateTime.now()
    ) :: Nil
  )

  val fakeAttribute: TenantAttribute = TenantAttribute(
    id = UUID.randomUUID,
    kind = TenantAttributeKind.CERTIFIED,
    assignmentTimestamp = OffsetDateTime.now()
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
      { implicit c: Seq[(String, String)] => tenantService.updateTenant("tenantId", TenantDelta(None, Nil)) }
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

}
