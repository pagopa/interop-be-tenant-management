package it.pagopa.interop.tenantmanagement.model.persistence

import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import java.util.UUID

sealed trait Event extends Persistable with ProjectableEvent

final case class TenantCreated(tenant: PersistentTenant)                    extends Event
final case class TenantUpdated(tenant: PersistentTenant)                    extends Event
final case class TenantDeleted(tenantId: String)                            extends Event
final case class SelfcareMappingCreated(selfcareId: String, tenantId: UUID) extends Event
final case class SelfcareMappingDeleted(selfcareId: String)                 extends Event
