package it.pagopa.interop.tenantmanagement.model.tenant

import java.util.UUID
import java.time.OffsetDateTime

final case class PersistentTenant(
  id: UUID,
  kind: Option[PersistentTenantKind],
  selfcareId: Option[String],
  externalId: PersistentExternalId,
  features: List[PersistentTenantFeature],
  attributes: List[PersistentTenantAttribute],
  createdAt: OffsetDateTime,
  updatedAt: Option[OffsetDateTime],
  mails: List[PersistentTenantMail],
  name: String
)
