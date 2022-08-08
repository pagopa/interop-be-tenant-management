package it.pagopa.interop.tenantmanagement.authz

import cats.implicits._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.Entity
import it.pagopa.interop.tenantmanagement.api.TenantApiService
import it.pagopa.interop.tenantmanagement.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantmanagement.api.impl.TenantApiServiceImpl
import it.pagopa.interop.tenantmanagement.model.persistence.Command
import it.pagopa.interop.tenantmanagement.model.{TenantSeed}
import it.pagopa.interop.tenantmanagement.server.impl.Main.tenantPersistenceEntity
import it.pagopa.interop.tenantmanagement.util.{AuthorizedRoutes, ClusteredMUnitRouteTest}

import java.util.UUID
import it.pagopa.interop.tenantmanagement.model.ExternalId
import java.time.OffsetDateTime
import it.pagopa.interop.tenantmanagement.model.TenantAttribute
import it.pagopa.interop.tenantmanagement.model.TenantAttributeKind

class TenantApiServiceAuthzSpec extends ClusteredMUnitRouteTest {
  override val testPersistentEntity: Entity[Command, ShardingEnvelope[Command]] = tenantPersistenceEntity

  val service: TenantApiService = TenantApiServiceImpl(testKit.system, testAkkaSharding, testPersistentEntity)

  test("Tenant api operation authorization spec should accept authorized roles for createTenant") {
    val endpoint = AuthorizedRoutes.endpoints("createTenant")
    val fakeSeed = TenantSeed(
      id = UUID.randomUUID().some,
      selfcareId = UUID.randomUUID(),
      externalId = ExternalId("IPA", "pippo"),
      kind = true,
      attributes = List(
        TenantAttribute(
          id = UUID.randomUUID(),
          kind = TenantAttributeKind.CERTIFIED,
          assignmentTimestamp = OffsetDateTime.now()
        )
      )
    )
    validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createTenant(fakeSeed) })
  }

  test("Tenant api operation authorization spec should accept authorized roles for getTenant") {
    val endpoint = AuthorizedRoutes.endpoints("getTenant")
    validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getTenant("fakeSeed") })
  }

}
