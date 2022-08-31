package it.pagopa.interop.tenantmanagement.api.impl

import it.pagopa.interop.tenantmanagement.api.AttributesApiService
import it.pagopa.interop.tenantmanagement.model.{Problem, Tenant}
import it.pagopa.interop.tenantmanagement.model.persistence.Command

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
import it.pagopa.interop.tenantmanagement.error.InternalErrors._
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors._
import it.pagopa.interop.tenantmanagement.model.persistence.Adapters._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.commons.utils.TypeConversions._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.model.TenantAttribute

class AttributesApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  offsetDateTimeSupplier: OffsetDateTimeSupplier
) extends AttributesApiService {

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

  override def addTenantAttribute(tenantId: String, tenantAttribute: TenantAttribute)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, M2M_ROLE, INTERNAL_ROLE) {
    val result: Future[PersistentTenant] = for {
      attribute     <- PersistentTenantAttribute.fromAPI(tenantAttribute).toFuture
      actorResponse <- commander(tenantId).askWithStatus(ref =>
        AddAttribute(tenantId, attribute, offsetDateTimeSupplier.get, ref)
      )
    } yield actorResponse

    onComplete(result) {
      case Success(tenant)                         =>
        addTenantAttribute200(tenant.toAPI)
      case Failure(ex @ AttributeAlreadyExists(_)) =>
        logger.error(s"Error while adding the attribute ${tenantAttribute}", ex)
        addTenantAttribute409(problemOf(StatusCodes.Conflict, AddAttributeConflict))
      case Failure(ex @ NotFoundTenant(_))         =>
        logger.error(s"Error while adding the attribute ${tenantAttribute}", ex)
        addTenantAttribute404(problemOf(StatusCodes.NotFound, GetTenantNotFound))
      case Failure(ex @ InvalidAttribute)          =>
        logger.error(s"Error while adding the attribute ${tenantAttribute}: Invalid Structure", ex)
        addTenantAttribute400(problemOf(StatusCodes.BadRequest, InvalidAttributeStructure))
      case Failure(ex)                             =>
        logger.error(s"Error while adding the attribute ${tenantAttribute}", ex)
        complete(problemOf(StatusCodes.InternalServerError, GenericError("Error while adding the attribute")))
    }
  }

  override def deleteTenantAttribute(tenantId: String, attributeId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, M2M_ROLE, INTERNAL_ROLE) {
    val result: Future[PersistentTenant] = for {
      attributeUUID <- attributeId.toFutureUUID
      actorResponse <- commander(tenantId).askWithStatus(ref =>
        DeleteAttribute(tenantId, attributeUUID, offsetDateTimeSupplier.get, ref)
      )
    } yield actorResponse

    onComplete(result) {
      case Success(tenant)                    =>
        addTenantAttribute200(tenant.toAPI)
      case Failure(ex @ NotFoundTenant(_))    =>
        logger.error(s"Error while deleting the attribute $attributeId", ex)
        addTenantAttribute404(problemOf(StatusCodes.NotFound, GetTenantNotFound))
      case Failure(ex @ NotFoundAttribute(_)) =>
        logger.error(s"Error while deleting the attribute $attributeId", ex)
        addTenantAttribute404(problemOf(StatusCodes.NotFound, AttributeNotFound))
      case Failure(ex)                        =>
        logger.error(s"Error while deleting the attribute $attributeId", ex)
        complete(problemOf(StatusCodes.InternalServerError, GenericError("Error while adding the attribute")))
    }
  }

  override def updateTenantAttribute(tenantId: String, attributeId: String, tenantAttribute: TenantAttribute)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = authorize(ADMIN_ROLE, M2M_ROLE) {
    val result: Future[PersistentTenant] = for {
      attrId        <- attributeId.toFutureUUID
      attribute     <- PersistentTenantAttribute.fromAPI(tenantAttribute).toFuture
      actorResponse <- commander(tenantId).askWithStatus(ref =>
        UpdateAttribute(tenantId, attrId, attribute, offsetDateTimeSupplier.get, ref)
      )
    } yield actorResponse

    onComplete(result) {
      case Success(tenant)                    =>
        addTenantAttribute200(tenant.toAPI)
      case Failure(ex @ NotFoundTenant(_))    =>
        logger.error(s"Error while updating the attribute $attributeId", ex)
        addTenantAttribute404(problemOf(StatusCodes.NotFound, GetTenantNotFound))
      case Failure(ex @ NotFoundAttribute(_)) =>
        logger.error(s"Error while updating the attribute $attributeId", ex)
        addTenantAttribute404(problemOf(StatusCodes.NotFound, AttributeNotFound))
      case Failure(ex @ InvalidAttribute)     =>
        logger.error(s"Error while adding the attribute ${tenantAttribute}: Invalid Structure", ex)
        addTenantAttribute400(problemOf(StatusCodes.BadRequest, InvalidAttributeStructure))
      case Failure(ex)                        =>
        logger.error(s"Error while updating the attribute $attributeId", ex)
        complete(problemOf(StatusCodes.InternalServerError, GenericError("Error while adding the attribute")))
    }
  }
}
