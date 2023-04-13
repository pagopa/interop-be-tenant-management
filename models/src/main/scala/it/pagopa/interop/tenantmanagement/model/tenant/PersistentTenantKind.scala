package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantKind

object PersistentTenantKind {
  case object Pa      extends PersistentTenantKind
  case object Gsp     extends PersistentTenantKind
  case object Private extends PersistentTenantKind
}
