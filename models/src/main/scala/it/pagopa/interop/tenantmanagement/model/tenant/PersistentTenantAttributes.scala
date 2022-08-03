package it.pagopa.interop.tenantmanagement.model.tenant

import java.time.OffsetDateTime
import java.util.UUID

object PersistentTenantAttribute

sealed trait PersistentTenantAttribute {
  def id: UUID
  def assignmentTimestamp: OffsetDateTime
  def revocationTimestamp: Option[OffsetDateTime]
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
  revocationTimestamp: Option[OffsetDateTime],
  extensionTimestamp: Option[OffsetDateTime],
  expirationTimestamp: OffsetDateTime
) extends PersistentTenantAttribute
