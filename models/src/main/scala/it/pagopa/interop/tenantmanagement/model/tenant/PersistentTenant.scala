package it.pagopa.interop.tenantmanagement.model.tenant

import java.util.UUID

final case class PersistentTenant(
  id: UUID,
  selfcareId: UUID,
  externalId: PersistentTenantExternalId,
  kind: Boolean,
  attributes: List[PersistentTenantAttribute]
)
