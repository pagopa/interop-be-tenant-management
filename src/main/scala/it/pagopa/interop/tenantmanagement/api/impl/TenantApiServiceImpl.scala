package it.pagopa.interop.tenantmanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.logging._
import it.pagopa.interop.commons.utils.AkkaUtils.getShard
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors._
import it.pagopa.interop.tenantmanagement.api.TenantApiService
import it.pagopa.interop.tenantmanagement.error.InternalErrors._
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors._
import it.pagopa.interop.tenantmanagement.model.persistence.Adapters._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

final case class TenantApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]]
) extends TenantApiService {

  private val logger                            = Logger.takingImplicit[ContextFieldsToLog](this.getClass())
  private implicit val timeout: Timeout         = Timeout(300.seconds)
  private val settings: ClusterShardingSettings = entity.settings.getOrElse(ClusterShardingSettings(system))
  // * For the moment we just call the commander (or we lift eithers)
  implicit val ec: ExecutionContextExecutor     = system.executionContext

  private def authorize(roles: String*)(
    route: => Route
  )(implicit contexts: Seq[(String, String)], toEntityMarshallerProblem: ToEntityMarshaller[Problem]): Route =
    authorizeInterop(hasPermissions(roles: _*), problemOf(StatusCodes.Forbidden, OperationForbidden))(route)

  private def commander(id: String): EntityRef[Command] =
    sharding.entityRefFor(TenantPersistentBehavior.TypeKey, getShard(id, settings.numberOfShards))

  override def createTenant(tenantSeed: TenantSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE, M2M_ROLE) {

    val result: Future[PersistentTenant] = for {
      tenant        <- PersistentTenant.fromAPI(tenantSeed).toFuture
      actorResponse <- commander(tenant.id.toString).askWithStatus(ref => CreateTenant(tenant, ref))
    } yield actorResponse

    onComplete(result) {
      case Success(tenant)                                  =>
        createTenant200(tenant.toAPI)
      case Failure(ex: TenantAlreadyExists)                 =>
        logger.error(s"Error while adding the tenant ${tenantSeed.id.toString}", ex)
        createTenant409(problemOf(StatusCodes.Conflict, CreateTenantConflict))
      case Failure(ex @ InvalidAttribute(attribute, field)) =>
        logger.error(s"Error while adding the tenant ${tenantSeed.id.toString}", ex)
        createTenant400(problemOf(StatusCodes.NotAcceptable, CreateTenantInvalidAttribute(attribute, field)))
      case Failure(ex)                                      =>
        logger.error(s"Error while adding the tenant ${tenantSeed.id.toString}", ex)
        complete(problemOf(StatusCodes.InternalServerError, GenericError("Error while adding the tenant")))
    }
  }

  override def getTenant(tenantId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, M2M_ROLE, SECURITY_ROLE) {

    val result: Future[PersistentTenant] = commander(tenantId).askWithStatus(ref => GetTenant(tenantId, ref))

    onComplete(result) {
      case Success(tenant)             => getTenant200(tenant.toAPI)
      case Failure(ex: TenantNotFound) =>
        logger.error(s"Error while adding the tenant $tenantId", ex)
        getTenant404(problemOf(StatusCodes.NotFound, GetTenantNotFound))
      case Failure(ex)                 =>
        logger.error(s"Error while adding the tenant $tenantId", ex)
        complete(problemOf(StatusCodes.InternalServerError, GenericError(ex.getMessage)))
    }
  }
}
