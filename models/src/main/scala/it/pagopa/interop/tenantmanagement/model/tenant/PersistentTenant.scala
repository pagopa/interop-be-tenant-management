package it.pagopa.interop.tenantmanagement.model.tenant

import java.util.UUID
import java.time.OffsetDateTime

final case class PersistentTenant(
  id: UUID,
  selfcareId: String,
  externalId: PersistentTenantExternalId,
  kinds: List[PersistentTenantKind],
  attributes: List[PersistentTenantAttribute],
  createdAt: OffsetDateTime,
  updatedAt: Option[OffsetDateTime]
)
