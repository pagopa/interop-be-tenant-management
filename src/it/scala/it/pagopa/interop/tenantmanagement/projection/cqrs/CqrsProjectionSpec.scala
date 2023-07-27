package it.pagopa.interop.tenantmanagement.projection.cqrs

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import it.pagopa.interop.tenantmanagement.ItSpecData.persistentTenant
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.{ItSpecConfiguration, ItSpecHelper}

import java.time.{OffsetDateTime, ZoneOffset}
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
        PersistentTenantDelta(
          id = tenant.id,
          selfcareId = Some(UUID.randomUUID().toString),
          features = List.empty,
          mails = Nil,
          kind = tenant.kind
        )
      )
      val persisted = findOne[PersistentTenant](expected.id.toString).futureValue

      compareTenants(expected, persisted)
    }

    "deserialized correctly PersistentTenantFeature" in {
      val features: List[PersistentTenantFeature] =
        List(PersistentTenantFeature.PersistentCertifier("CERT"))

      val expected  = createTenant(persistentTenant.copy(features = features))
      val persisted = findOne[PersistentTenant](expected.id.toString).futureValue

      compareTenants(expected, persisted)
    }

    "deserialized correctly PersistentTenantAttribute" in {

      val assignmentTimestamp: OffsetDateTime = OffsetDateTime.of(2022, 12, 31, 11, 22, 34, 0, ZoneOffset.UTC)
      val revocationTimestamp: OffsetDateTime = OffsetDateTime.of(2022, 12, 31, 11, 22, 35, 0, ZoneOffset.UTC)
      val verificationDate: OffsetDateTime    = OffsetDateTime.of(2022, 12, 31, 11, 22, 36, 0, ZoneOffset.UTC)

      val attributes: List[PersistentTenantAttribute] =
        List(
          PersistentCertifiedAttribute(
            id = UUID.randomUUID(),
            assignmentTimestamp = assignmentTimestamp,
            revocationTimestamp = Some(revocationTimestamp)
          ),
          PersistentDeclaredAttribute(
            id = UUID.randomUUID(),
            assignmentTimestamp = assignmentTimestamp,
            revocationTimestamp = Some(revocationTimestamp)
          ),
          PersistentVerifiedAttribute(
            id = UUID.randomUUID(),
            assignmentTimestamp = assignmentTimestamp,
            verifiedBy = List(
              PersistentTenantVerifier(
                id = UUID.randomUUID(),
                verificationDate = verificationDate,
                expirationDate = None,
                extensionDate = None
              )
            ),
            revokedBy = List.empty
          )
        )

      val expected  = createTenant(persistentTenant.copy(attributes = attributes))
      val persisted = findOne[PersistentTenant](expected.id.toString).futureValue

      compareTenants(expected, persisted)
    }

  }

}
