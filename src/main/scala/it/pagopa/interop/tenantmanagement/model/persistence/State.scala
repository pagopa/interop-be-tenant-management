package it.pagopa.interop.tenantmanagement.model.persistence

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import java.util.UUID

final case class State(tenants: Map[String, PersistentTenant], selfcareMappings: Map[String, UUID])
    extends Persistable {
  def addTenant(tenant: PersistentTenant): State = copy(tenants = tenants + (tenant.id.toString -> tenant))
  def allTenants: List[PersistentTenant]         = tenants.values.toList.sortBy(_.id)
  def addSelfcareMapping(selfcareId: String, tenantId: UUID): State =
    copy(selfcareMappings = selfcareMappings + (selfcareId -> tenantId))
  def getTenantIdBySelfcareId(selfcareId: String): Option[UUID]     = selfcareMappings.get(selfcareId)

  def deleteTenant(tenantId: String): State = State(
    tenants - tenantId,
    tenants.get(tenantId).flatMap(_.selfcareId).fold(selfcareMappings)(selfcareId => selfcareMappings - selfcareId)
  )

  def deleteSelfcareMapping(selfcareId: String): State = copy(selfcareMappings = selfcareMappings - selfcareId)

  def addTenantMail(tenant: PersistentTenant): State = copy(tenants = tenants + (tenant.id.toString -> tenant))

  def deleteTenantMail(tenantId: UUID, mailId: String): State = {
    tenants.get(tenantId.toString) match {
      case Some(tenant) => {
        val updatedTenant = tenant.copy(mails = tenant.mails.filterNot(_.id == mailId))
        copy(tenants = tenants + (tenant.id.toString -> updatedTenant))
      }
      case None         => this
    }
  }
}

object State {
  val empty: State = State(Map.empty[String, PersistentTenant], Map.empty[String, UUID])
}
