package it.pagopa.interop.tenantmanagement.projection.cqrs

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import it.pagopa.interop.tenantmanagement.ItSpecData.persistentTenant
import it.pagopa.interop.tenantmanagement.{ItSpecConfiguration, ItSpecHelper}
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentTenant, PersistentTenantDelta}
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import java.util.UUID

class CqrsProjectionSpec extends ScalaTestWithActorTestKit(ItSpecConfiguration.config) with ItSpecHelper {

  "Projection" should {
    "succeed for event TenantCreated" in {
      val expected  = createTenant(persistentTenant)
      val persisted = findOne[PersistentTenant](expected.id.toString).futureValue

      compareTenants(expected, persisted)
    }

    "succeed for event TenantUpdated" in {
      val tenant    = createTenant(persistentTenant.copy(id = UUID.randomUUID()))
      val expected  = updateTenant(
        PersistentTenantDelta(id = tenant.id, selfcareId = Some(UUID.randomUUID().toString), features = None)
      )
      val persisted = findOne[PersistentTenant](expected.id.toString).futureValue

      compareTenants(expected, persisted)
    }

  }

}
