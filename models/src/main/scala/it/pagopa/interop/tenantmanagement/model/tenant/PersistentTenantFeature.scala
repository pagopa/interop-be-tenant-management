package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantFeature extends Product

object PersistentTenantFeature {
  final case class PersistentCertifier(certifierId: String) extends PersistentTenantFeature
}
