package it.pagopa.interop.tenantmanagement.model.persistence

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

sealed trait Command

case object Idle                                                                                        extends Command
final case class CreateTenant(tenant: PersistentTenant, replyTo: ActorRef[StatusReply[PersistentTenant]])
    extends Command
final case class GetTenant(tenantId: String, replyTo: ActorRef[StatusReply[PersistentTenant]])          extends Command
final case class GetTenants(from: Int, to: Int, replyTo: ActorRef[StatusReply[List[PersistentTenant]]]) extends Command

object Command {
  def getTenantsCommandIterator(sliceSize: Int): Iterator[ActorRef[StatusReply[List[PersistentTenant]]] => GetTenants] =
    Iterator
      .from(0, sliceSize)
      .map(n => GetTenants(n, n + sliceSize + 1, _))
}
