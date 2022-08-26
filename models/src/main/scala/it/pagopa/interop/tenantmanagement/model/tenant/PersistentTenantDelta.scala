package it.pagopa.interop.tenantmanagement.model.tenant

final case class PersistentTenantDelta(id: String, selfcareId: Option[String], features: List[PersistentTenantFeature])
