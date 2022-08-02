package it.pagopa.interop.tenantmanagement.authz

import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.Entity
import it.pagopa.interop.tenantmanagement.api.TenantApiService
import it.pagopa.interop.tenantmanagement.api.impl.TenantApiMarshallerImpl._
import it.pagopa.interop.tenantmanagement.api.impl.TenantApiServiceImpl
import it.pagopa.interop.tenantmanagement.model.persistence.Command
import it.pagopa.interop.tenantmanagement.model.{TenantAttributes, TenantSeed}
import it.pagopa.interop.tenantmanagement.server.impl.Main.tenantPersistenceEntity
import it.pagopa.interop.tenantmanagement.util.{AuthorizedRoutes, ClusteredScalatestRouteTest}
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class TenantApiServiceAuthzSpec extends AnyWordSpecLike with ClusteredScalatestRouteTest {
  override val testPersistentEntity: Entity[Command, ShardingEnvelope[Command]] = tenantPersistenceEntity

  val service: TenantApiService = TenantApiServiceImpl(testTypedSystem, testAkkaSharding, testPersistentEntity)

  "Tenant api operation authorization spec" should {

    "accept authorized roles for createTenant" in {
      val endpoint = AuthorizedRoutes.endpoints("createTenant")
      val fakeSeed = TenantSeed(
        id = UUID.randomUUID(),
        attributes = TenantAttributes(certified = Seq.empty, declared = Seq.empty, verified = Seq.empty)
      )
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.createTenant(fakeSeed) })
    }
    "accept authorized roles for getTenant" in {
      val endpoint = AuthorizedRoutes.endpoints("getTenant")
      validateAuthorization(endpoint, { implicit c: Seq[(String, String)] => service.getTenant("fakeSeed") })
    }

  }

}
