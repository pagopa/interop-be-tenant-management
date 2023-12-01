package it.pagopa.interop.tenantmanagement.model.tenant

import java.time.OffsetDateTime
import java.util.UUID

final case class PersistentTenantDelta(
  id: UUID,
  selfcareId: Option[String],
  features: List[PersistentTenantFeature],
  kind: Option[PersistentTenantKind],
  onboardedAt: Option[OffsetDateTime],
  subUnitType: Option[PersistentTenantUnitType]
)
