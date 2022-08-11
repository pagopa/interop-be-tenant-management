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
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import cats.implicits._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantExternalId

final case class TenantApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  offsetDateTimeSupplier: OffsetDateTimeSupplier
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

  private def commanders: List[EntityRef[Command]] =
    (0 until settings.numberOfShards).map(_.toString).toList.map(commander)

  override def createTenant(tenantSeed: TenantSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, SECURITY_ROLE, M2M_ROLE) {

    val result: Future[PersistentTenant] = for {
      tenant        <- PersistentTenant.fromAPI(tenantSeed, offsetDateTimeSupplier).toFuture
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
        logger.error(s"Error while getting the tenant $tenantId", ex)
        getTenant404(problemOf(StatusCodes.NotFound, GetTenantNotFound))
      case Failure(ex)                 =>
        logger.error(s"Error while getting the tenant $tenantId", ex)
        complete(problemOf(StatusCodes.InternalServerError, GenericError(ex.getMessage)))
    }
  }

  override def getTenantByExternalId(origin: String, code: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, API_ROLE, M2M_ROLE, SECURITY_ROLE) {

    val externalId: ExternalId                   = ExternalId(origin, code)
    val result: Future[Option[PersistentTenant]] = findFirstTenantWithExtenalId(externalId)

    onComplete(result) {
      case Success(Some(tenant))           => getTenantByExternalId200(tenant.toAPI)
      case Success(None)                   =>
        logger.info(s"Tenant with externalId $externalId not found")
        getTenant404(problemOf(StatusCodes.NotFound, GetTenantNotFound))
      case Failure(NonUniqueTenantMapping) =>
        complete(problemOf(StatusCodes.InternalServerError, GenericError("Internal server error")))
      case Failure(ex)                     =>
        logger.error(s"Error while getting the tenant with externalId $externalId", ex)
        complete(problemOf(StatusCodes.InternalServerError, GenericError(ex.getMessage)))
    }
  }

  private def findFirstTenantWithExtenalId(
    externalId: ExternalId
  )(implicit contexts: Seq[(String, String)]): Future[Option[PersistentTenant]] =
    findFirstTenant(_.externalId == PersistentTenantExternalId.fromAPI(externalId)).flatMap {
      case Nil           => Future.successful(none[PersistentTenant])
      case tenant :: Nil => Future.successful(tenant.some)
      case ts            =>
        logger.error(s"MORE THAN ONE TENANT CORRESPONDING TO $externalId :\n ${ts.mkString("\n")}")
        Future.failed(NonUniqueTenantMapping)
    }

  private def findFirstTenant(matchCriteria: PersistentTenant => Boolean): Future[List[PersistentTenant]] =
    commanders.traverseFilter(findFirstInShard(_, matchCriteria))

  private def findFirstInShard(
    commander: EntityRef[Command],
    matchCriteria: PersistentTenant => Boolean
  ): Future[Option[PersistentTenant]] = {
    val commandIterator = Command.getTenantsCommandIterator(100)

    // It's stack safe since every submission to an Execution context resets the stack, creating a trampoline effect
    def loop: Future[Option[PersistentTenant]] = commander.askWithStatus(commandIterator.next()).flatMap { slice =>
      if (slice.isEmpty) Future.successful(None)
      else slice.find(matchCriteria).map(_.some).map(Future.successful).getOrElse(loop)
    }

    loop
  }

}
