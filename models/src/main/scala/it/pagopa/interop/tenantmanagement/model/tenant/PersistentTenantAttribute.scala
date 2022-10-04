package it.pagopa.interop.tenantmanagement.model.tenant

import java.time.OffsetDateTime
import java.util.UUID

object PersistentTenantAttribute

sealed trait PersistentTenantAttribute extends Product {
  def id: UUID
  def assignmentTimestamp: OffsetDateTime
}

final case class PersistentCertifiedAttribute(
  id: UUID,
  assignmentTimestamp: OffsetDateTime,
  revocationTimestamp: Option[OffsetDateTime]
) extends PersistentTenantAttribute

final case class PersistentDeclaredAttribute(
  id: UUID,
  assignmentTimestamp: OffsetDateTime,
  revocationTimestamp: Option[OffsetDateTime]
) extends PersistentTenantAttribute

final case class PersistentVerifiedAttribute(
  id: UUID,
  assignmentTimestamp: OffsetDateTime,
  verifiedBy: List[PersistentTenantVerifier],
  revokedBy: List[PersistentTenantRevoker]
) extends PersistentTenantAttribute
