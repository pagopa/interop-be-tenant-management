package it.pagopa.interop.tenantmanagement.model.persistence

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

final case class State(tenants: Map[String, PersistentTenant]) extends Persistable {
  def addTenant(tenant: PersistentTenant): State = copy(tenants = tenants + (tenant.id.toString -> tenant))
}

object State {
  val empty: State = State(tenants = Map.empty[String, PersistentTenant])
}
