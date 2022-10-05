package it.pagopa.interop.tenantmanagement.model.tenant

import java.time.OffsetDateTime
import java.util.UUID

final case class PersistentTenantRevoker(
  id: UUID,
  verificationDate: OffsetDateTime,
  renewal: PersistentVerificationRenewal,
  expirationDate: Option[OffsetDateTime],
  extensionDate: Option[OffsetDateTime],
  revocationDate: OffsetDateTime
)
