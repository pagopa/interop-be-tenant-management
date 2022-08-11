package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantFeature

object PersistentTenantFeature {
  final case class PersistentCertifier(certifierId: String) extends PersistentTenantFeature
}
// case class TenantKind(certifier: Option[Certifier])
// case class Certifier(certifierId: String)
