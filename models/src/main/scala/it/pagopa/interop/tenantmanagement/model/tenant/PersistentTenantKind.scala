package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantKind

object PersistentTenantKind {
  case object STANDARD  extends PersistentTenantKind
  case object CERTIFIER extends PersistentTenantKind
}
