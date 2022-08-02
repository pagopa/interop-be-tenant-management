package it.pagopa.interop.tenantmanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.AkkaUtils.getShard
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.{GenericError, OperationForbidden}
import it.pagopa.interop.tenantmanagement.api.TenantApiService
import it.pagopa.interop.tenantmanagement.common.system._
import it.pagopa.interop.tenantmanagement.error.InternalErrors.{TenantAlreadyExists, TenantNotFound}
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors.{
  CreateTenantBadRequest,
  CreateTenantConflict,
  GetTenantBadRequest,
  GetTenantNotFound
}
import it.pagopa.interop.tenantmanagement.model.persistence.Adapters._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantmanagement.model.{Problem, Tenant, TenantSeed}

import scala.concurrent.Future
import scala.util.{Failure, Success}

final case class TenantApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
) extends TenantApiService {

  private val logger = Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  private val settings: ClusterShardingSettings = entity.settings match {
    case None    => ClusterShardingSettings(system)
    case Some(s) => s
  }

  private[this] def authorize(roles: String*)(
    route: => Route
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorizeInterop(hasPermissions(roles: _*), problemOf(StatusCodes.Forbidden, OperationForbidden)) {
      route
    }

  /**
   * Code: 201, Message: Tenant created, DataType: Tenant
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 409, Message: Tenant already exists, DataType: Problem
   */
  override def createTenant(tenantSeed: TenantSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE) {

    val commander: EntityRef[Command] =
      sharding.entityRefFor(TenantPersistentBehavior.TypeKey, getShard(tenantSeed.id.toString, settings.numberOfShards))

    val result: Future[StatusReply[PersistentTenant]] =
      commander.ask(ref => CreateTenant(PersistentTenant.fromSeed(tenantSeed), ref))

    onComplete(result) {
      case Success(statusReply) if statusReply.isSuccess =>
        createTenant200(statusReply.getValue.toAPI)
      case Success(statusReply)                          =>
        statusReply.getError match {
          case ex: TenantAlreadyExists =>
            logger.error(s"Error while adding the tenant ${tenantSeed.id.toString}", ex)
            createTenant409(problemOf(StatusCodes.Conflict, CreateTenantConflict))
          case ex                      =>
            logger.error(s"Error while adding the tenant ${tenantSeed.id.toString}", ex)
            complete(problemOf(StatusCodes.InternalServerError, GenericError(ex.getMessage)))
        }
      case Failure(ex)                                   =>
        logger.error(s"Error while adding the tenant ${tenantSeed.id.toString}", ex)
        createTenant400(problemOf(StatusCodes.BadRequest, CreateTenantBadRequest))
    }
  }

  /**
   * Code: 200, Message: Tenant created, DataType: Tenant
   * Code: 400, Message: Invalid input, DataType: Problem
   * Code: 404, Message: Tenant not found, DataType: Problem
   */
  override def getTenant(tenantId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, M2M_ROLE, SECURITY_ROLE) {

    val commander: EntityRef[Command] =
      sharding.entityRefFor(TenantPersistentBehavior.TypeKey, getShard(tenantId, settings.numberOfShards))

    val result: Future[StatusReply[PersistentTenant]] = commander.ask(ref => GetTenant(tenantId, ref))

    onComplete(result) {
      case Success(statusReply) if statusReply.isSuccess =>
        getTenant200(statusReply.getValue.toAPI)
      case Success(statusReply)                          =>
        statusReply.getError match {
          case ex: TenantNotFound =>
            logger.error(s"Error while adding the tenant $tenantId", ex)
            getTenant404(problemOf(StatusCodes.NotFound, GetTenantNotFound))
          case ex                 =>
            logger.error(s"Error while adding the tenant $tenantId", ex)
            complete(problemOf(StatusCodes.InternalServerError, GenericError(ex.getMessage)))
        }
      case Failure(ex)                                   =>
        logger.error(s"Error while adding the tenant $tenantId", ex)
        getTenant400(problemOf(StatusCodes.BadRequest, GetTenantBadRequest))
    }
  }
}
