package it.pagopa.interop.tenantmanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.interop.tenantmanagement.model.tenant._
import java.util.UUID
import java.time.OffsetDateTime

sealed trait Command

case object Idle                                                                               extends Command
final case class CreateTenant(tenant: PersistentTenant, replyTo: ActorRef[StatusReply[PersistentTenant]])
    extends Command
final case class GetTenant(tenantId: String, replyTo: ActorRef[StatusReply[PersistentTenant]]) extends Command
final case class GetTenantsWithExternalId(
  externalId: PersistentExternalId,
  replyTo: ActorRef[StatusReply[List[PersistentTenant]]]
) extends Command

final case class UpdateTenant(
  tenantDelta: PersistentTenantDelta,
  timestamp: OffsetDateTime,
  replyTo: ActorRef[StatusReply[PersistentTenant]]
) extends Command

final case class AddAttribute(
  tenantId: String,
  persistentAttribute: PersistentTenantAttribute,
  dateTime: OffsetDateTime,
  replyTo: ActorRef[StatusReply[PersistentTenant]]
) extends Command

final case class UpdateAttribute(
  tenantId: String,
  attributeId: UUID,
  persistentAttribute: PersistentTenantAttribute,
  dateTime: OffsetDateTime,
  replyTo: ActorRef[StatusReply[PersistentTenant]]
) extends Command

final case class GetTenantBySelfcareId(selfcareId: String, replyTo: ActorRef[StatusReply[UUID]]) extends Command

final case class AddSelfcareIdTenantMapping(selfcareId: String, tenantId: UUID, replyTo: ActorRef[StatusReply[Unit]])
    extends Command

final case class DeleteTenant(tenantId: String, replyTo: ActorRef[StatusReply[Unit]]) extends Command

final case class DeleteSelfcareIdTenantMapping(selfcareId: String, replyTo: ActorRef[StatusReply[Unit]]) extends Command
