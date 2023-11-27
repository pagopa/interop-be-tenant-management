package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantUnitType

object PersistentTenantUnitType {
  case object Aoo extends PersistentTenantUnitType
  case object Uo  extends PersistentTenantUnitType
}
