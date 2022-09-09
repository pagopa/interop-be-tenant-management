package it.pagopa.interop.tenantmanagement.model.persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.pattern.StatusReply._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import it.pagopa.interop.tenantmanagement.error.InternalErrors._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantmanagement.model.persistence.Adapters._

import cats.implicits._
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
      case GetTenant(tenantId, replyTo) =>
        val maybeTenant: Option[PersistentTenant] = state.tenants.get(tenantId)
        val response = maybeTenant.fold(error[PersistentTenant](NotFoundTenant(tenantId)))(success)
        Effect.reply(replyTo)(response)

      case GetTenantsWithExternalId(externalId, replyTo) =>
        val matchingTenants: List[PersistentTenant] = state.allTenants.filter(_.externalId == externalId)
        Effect.reply(replyTo)(success(matchingTenants))

      case CreateTenant(newTenant, replyTo) =>
        val maybeTenantId: Option[String]         = state.tenants.get(newTenant.id.toString).map(_.id.toString())
        val success: Effect[TenantCreated, State] = persistAndReply(newTenant, TenantCreated)(replyTo)
        val failure: String => Effect[TenantCreated, State] = id => fail(TenantAlreadyExists(id))(replyTo)
        maybeTenantId.fold(success)(failure)

      case UpdateTenant(tenantDelta, replyTo) =>
        val maybeTenant: Option[PersistentTenant]                     =
          state.tenants.get(tenantDelta.id.toString).map(_.update(tenantDelta))
        val success: PersistentTenant => Effect[TenantUpdated, State] = t => persistAndReply(t, TenantUpdated)(replyTo)
        val failure: Effect[TenantUpdated, State] = fail(NotFoundTenant(tenantDelta.id.toString))(replyTo)
        maybeTenant.fold(failure)(success)

      case AddAttribute(tenantId, attribute, dateTime, replyTo) =>
        val result: Either[Throwable, PersistentTenant] = for {
          maybeTenant <- state.tenants.get(tenantId).toRight(NotFoundTenant(tenantId))
          _           <- maybeTenant
            .getAttribute(attribute.id)
            .fold(Either.unit[Throwable])(_ => AttributeAlreadyExists(attribute.id.toString).asLeft[Unit])
        } yield maybeTenant.addAttribute(attribute, dateTime)
        result.fold(fail(_)(replyTo), t => persistAndReply(t, TenantUpdated)(replyTo))

      case UpdateAttribute(tenantId, attributeId, attribute, dateTime, replyTo) =>
        val result: Either[Throwable, PersistentTenant] = for {
          maybeTenant <- state.tenants.get(tenantId).toRight(NotFoundTenant(tenantId))
          _           <- maybeTenant.getAttribute(attributeId).toRight(NotFoundAttribute(attribute.id.toString))
        } yield maybeTenant.deleteAttribute(attributeId, dateTime).addAttribute(attribute, dateTime)
        result.fold(fail(_)(replyTo), t => persistAndReply(t, TenantUpdated)(replyTo))

      case DeleteAttribute(tenantId, attributeId, dateTime, replyTo) =>
        val result: Either[Throwable, PersistentTenant] = for {
          maybeTenant <- state.tenants.get(tenantId).toRight(NotFoundTenant(tenantId))
          _           <- maybeTenant.getAttribute(attributeId).toRight(NotFoundAttribute(attributeId.toString))
        } yield maybeTenant.deleteAttribute(attributeId, dateTime)
        result.fold(fail(_)(replyTo), t => persistAndReply(t, TenantUpdated)(replyTo))

      case Idle =>
        context.log.debug(s"Passivate shard: ${shard.path.name}")
        Effect.reply(shard)(ClusterSharding.Passivate(context.self))
    }
  }

  def fail[T](ex: Throwable)(replyTo: ActorRef[StatusReply[PersistentTenant]]): Effect[T, State] =
    Effect.reply(replyTo)(error[PersistentTenant](ex))

  def persistAndReply[T](tenant: PersistentTenant, eventBuilder: PersistentTenant => T)(
    replyTo: ActorRef[StatusReply[PersistentTenant]]
  ): Effect[T, State] = Effect.persist(eventBuilder(tenant)).thenReply(replyTo)((_: State) => success(tenant))

  private val eventHandler: (State, Event) => State = (state, event) =>
    event match {
      case TenantCreated(tenant) => state.addTenant(tenant)
      case TenantUpdated(tenant) => state.addTenant(tenant)
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
    ).withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = numberOfEvents, keepNSnapshots = 10))
      .withTagger(_ => Set(persistenceTag))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200 millis, 5 seconds, 0.1))
  }

}
