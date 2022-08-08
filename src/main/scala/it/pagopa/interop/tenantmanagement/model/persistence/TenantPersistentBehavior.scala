package it.pagopa.interop.tenantmanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.interop.tenantmanagement.error.InternalErrors.{TenantAlreadyExists, TenantNotFound}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.language.postfixOps

object TenantPersistentBehavior {

  def commandHandler(
    shard: ActorRef[ClusterSharding.ShardCommand],
    context: ActorContext[Command]
  ): (State, Command) => Effect[Event, State] = { (state, command) =>
    val idleTimeout = context.system.settings.config.getDuration("tenant-management.idle-timeout")
    context.setReceiveTimeout(idleTimeout.get(ChronoUnit.SECONDS) seconds, Idle)
    command match {
      case CreateTenant(newTenant, replyTo) =>
        val tenant: Option[PersistentTenant] = state.tenants.get(newTenant.id.toString)
        tenant.fold(
          Effect
            .persist(TenantCreated(newTenant))
            .thenRun { (_: State) => replyTo ! StatusReply.Success(newTenant) }
        ) { p =>
          replyTo ! StatusReply.Error[PersistentTenant](TenantAlreadyExists(p.id.toString))
          Effect.none[TenantCreated, State]
        }

      case GetTenant(tenantId, replyTo) =>
        val tenant: Option[PersistentTenant] = state.tenants.get(tenantId)
        tenant.fold {
          replyTo ! StatusReply.Error[PersistentTenant](TenantNotFound(tenantId))
          Effect.none[Event, State]
        } { p =>
          replyTo ! StatusReply.Success[PersistentTenant](p)
          Effect.none[Event, State]
        }

      case Idle =>
        shard ! ClusterSharding.Passivate(context.self)
        context.log.debug(s"Passivate shard: ${shard.path.name}")
        Effect.none[Event, State]
    }
  }

  def handleFailure[T](ex: Throwable)(replyTo: ActorRef[StatusReply[PersistentTenant]]): EffectBuilder[T, State] = {
    replyTo ! StatusReply.Error[PersistentTenant](ex)
    Effect.none[T, State]
  }

  def persistStateAndReply[T](tenant: PersistentTenant, eventBuilder: PersistentTenant => T)(
    replyTo: ActorRef[StatusReply[PersistentTenant]]
  ): EffectBuilder[T, State] = Effect
    .persist(eventBuilder(tenant))
    .thenRun((_: State) => replyTo ! StatusReply.Success(tenant))

  private val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case TenantCreated(tenant) => state.addTenant(tenant)
    }

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("interop-be-tenant-management-persistence")

  def apply(
    shard: ActorRef[ClusterSharding.ShardCommand],
    persistenceId: PersistenceId,
    persistenceTag: String
  ): Behavior[Command] = Behaviors.setup { context =>
    context.log.debug(s"Starting Tenant Shard ${persistenceId.id}")
    val numberOfEvents = context.system.settings.config.getInt("tenant-management.number-of-events-before-snapshot")
    EventSourcedBehavior[Command, Event, State](
      persistenceId = persistenceId,
      emptyState = State.empty,
      commandHandler = commandHandler(shard, context),
      eventHandler = eventHandler
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 1))
      .withTagger(_ => Set(persistenceTag))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
  }

}
