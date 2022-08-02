package it.pagopa.interop.tenantmanagement.model.tenant

import java.util.UUID

object PersistentTenant
final case class PersistentTenant(id: UUID, certifier: Boolean, attributes: PersistentTenantAttributes)
