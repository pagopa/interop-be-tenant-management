package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantMailKind

object PersistentTenantMailKind {
  case object TechSupportMail extends PersistentTenantMailKind
}
