package it.pagopa.interop.tenantmanagement.model.persistence

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import java.util.UUID

final case class State(tenants: Map[String, PersistentTenant], selfcareMappings: Map[String, UUID])
    extends Persistable {
  def addTenant(tenant: PersistentTenant): State = copy(tenants = tenants + (tenant.id.toString -> tenant))
  def allTenants: List[PersistentTenant]         = tenants.values.toList.sortBy(_.id)
  def addSelfcareMapping(selfcareId: String, tenantId: UUID): State       =
    copy(selfcareMappings = selfcareMappings + (selfcareId -> tenantId))
  def getTenantBySelfcareId(selfcareId: String): Option[PersistentTenant] =
    selfcareMappings.get(selfcareId).map(_.toString()).flatMap(tenants.get)
}

object State {
  val empty: State = State(Map.empty[String, PersistentTenant], Map.empty[String, UUID])
}
