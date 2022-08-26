package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentVerificationRenewal

object PersistentVerificationRenewal {
  case object REVOKE_ON_EXPIRATION extends PersistentVerificationRenewal
  case object AUTOMATIC_RENEWAL    extends PersistentVerificationRenewal
}
