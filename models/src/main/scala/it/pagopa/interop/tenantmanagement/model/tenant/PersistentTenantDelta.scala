package it.pagopa.interop.tenantmanagement.model.tenant

import java.util.UUID

final case class PersistentTenantDelta(
  id: UUID,
  selfcareId: Option[String],
  features: Option[List[PersistentTenantFeature]]
)
