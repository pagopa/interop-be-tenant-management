package it.pagopa.interop.tenantmanagement.model.tenant

import java.util.UUID

final case class PersistentTenant(
  id: UUID,
  selfcareId: String,
  externalId: PersistentTenantExternalId,
  kinds: List[PersistentTenantKind],
  attributes: List[PersistentTenantAttribute]
)
