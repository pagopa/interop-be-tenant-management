package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantMailKind

object PersistentTenantMailKind {
  case object ContactEmail   extends PersistentTenantMailKind
  case object DigitalAddress extends PersistentTenantMailKind
}
