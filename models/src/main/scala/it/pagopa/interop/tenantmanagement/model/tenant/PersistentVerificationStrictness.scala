package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentVerificationStrictness

object PersistentVerificationStrictness {
  case object STRICT   extends PersistentVerificationStrictness
  case object STANDARD extends PersistentVerificationStrictness
}
