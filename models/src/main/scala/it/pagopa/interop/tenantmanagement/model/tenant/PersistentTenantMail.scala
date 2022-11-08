package it.pagopa.interop.tenantmanagement.model.tenant

import java.time.OffsetDateTime

final case class PersistentTenantMail(
  kind: PersistentTenantMailKind,
  address: String,
  description: String,
  createdAt: OffsetDateTime
)
