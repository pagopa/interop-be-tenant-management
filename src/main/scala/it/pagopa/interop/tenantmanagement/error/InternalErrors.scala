package it.pagopa.interop.tenantmanagement.error

object InternalErrors {
  final case class TenantNotFound(tenantId: String)      extends Throwable(s"Tenant $tenantId not found")
  final case class TenantAlreadyExists(tenantId: String) extends Throwable(s"Tenant $tenantId already exists")

}
