package it.pagopa.interop.tenantmanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.interop.tenantmanagement.model.tenant._

sealed trait Command

case object Idle                                                                               extends Command
final case class CreateTenant(tenant: PersistentTenant, replyTo: ActorRef[StatusReply[PersistentTenant]])
    extends Command
final case class GetTenant(tenantId: String, replyTo: ActorRef[StatusReply[PersistentTenant]]) extends Command
final case class GetTenantsWithExternalId(
  externalId: PersistentExternalId,
  replyTo: ActorRef[StatusReply[List[PersistentTenant]]]
) extends Command

final case class UpdateTenant(tenantDelta: PersistentTenantDelta, replyTo: ActorRef[StatusReply[PersistentTenant]])
    extends Command
