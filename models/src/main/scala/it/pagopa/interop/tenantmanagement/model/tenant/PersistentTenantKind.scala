package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantKind

object PersistentTenantKind {
  case object PA      extends PersistentTenantKind
  case object GSP     extends PersistentTenantKind
  case object PRIVATE extends PersistentTenantKind
}
