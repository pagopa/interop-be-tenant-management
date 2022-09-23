package it.pagopa.interop.tenantmanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.interop.tenantmanagement.model.tenant._
import java.util.UUID
import java.time.OffsetDateTime
import akka.Done

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

final case class DeleteAttribute(
  tenantId: String,
  attributeId: UUID,
  dateTime: OffsetDateTime,
  replyTo: ActorRef[StatusReply[PersistentTenant]]
) extends Command

final case class GetTenantIdBySelfcareId(selfcareId: String, replyTo: ActorRef[StatusReply[PersistentTenant]])
    extends Command

final case class AddSelfcareIdTenantMapping(
  selfcareId: String,
  tenantId: UUID,
  replyTo: ActorRef[StatusReply[Done.type]]
) extends Command
