package it.pagopa.interop.tenantmanagement

import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentExternalId, PersistentTenant}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantKind

object ItSpecData {
  final val timestamp: OffsetDateTime = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 0, ZoneOffset.UTC)

  def persistentTenant: PersistentTenant = PersistentTenant(
    id = UUID.randomUUID(),
    kind = Option(PersistentTenantKind.PA),
    selfcareId = None,
    externalId = PersistentExternalId(origin = "origin", value = "value"),
    features = List.empty,
    attributes = List.empty,
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = "name"
  )
}
