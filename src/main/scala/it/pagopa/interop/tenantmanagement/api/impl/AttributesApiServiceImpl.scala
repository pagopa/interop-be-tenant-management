package it.pagopa.interop.tenantmanagement.api.impl

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.onComplete
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging._
import it.pagopa.interop.commons.utils.AkkaUtils.getShard
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.api.AttributesApiService
import it.pagopa.interop.tenantmanagement.api.impl.AttributesApiResponseHandlers._
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors._
import it.pagopa.interop.tenantmanagement.model.persistence.Adapters._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model.{Problem, Tenant, TenantAttribute}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class AttributesApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  offsetDateTimeSupplier: OffsetDateTimeSupplier
)(implicit ec: ExecutionContext)
    extends AttributesApiService {

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)
  private implicit val timeout: Timeout                                 = Timeout(300.seconds)
  private val settings: ClusterShardingSettings = entity.settings.getOrElse(ClusterShardingSettings(system))

  private def commander(id: String): EntityRef[Command] =
    sharding.entityRefFor(TenantPersistentBehavior.TypeKey, getShard(id, settings.numberOfShards))

  override def getTenantAttribute(tenantId: String, attributeId: String)(implicit
    toEntityMarshallerTenantAttribute: ToEntityMarshaller[TenantAttribute],
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving Attribute $attributeId for Tenant $tenantId"
    logger.info(operationLabel)

    val result: Future[TenantAttribute] = for {
      tenant    <- commander(tenantId).askWithStatus(ref => GetTenant(tenantId, ref))
      attribute <- tenant.attributes.find(_.id.toString == attributeId).toFuture(AttributeNotFound(attributeId))
    } yield attribute.toAPI

    onComplete(result) { getTenantAttributeResponse[TenantAttribute](operationLabel)(getTenantAttribute200) }
  }

  override def addTenantAttribute(tenantId: String, tenantAttribute: TenantAttribute)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Adding Attribute Tenant $tenantId"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      attribute     <- PersistentTenantAttribute.fromAPI(tenantAttribute).toFuture
      actorResponse <- commander(tenantId).askWithStatus(ref =>
        AddAttribute(tenantId, attribute, offsetDateTimeSupplier.get(), ref)
      )
    } yield actorResponse.toAPI

    onComplete(result) { addTenantAttributeResponse[Tenant](operationLabel)(addTenantAttribute200) }
  }

  override def updateTenantAttribute(tenantId: String, attributeId: String, tenantAttribute: TenantAttribute)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Updating Attribute $attributeId Tenant $tenantId"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      attrId        <- attributeId.toFutureUUID
      attribute     <- PersistentTenantAttribute
        .fromAPI(tenantAttribute)
        .ensure(InvalidAttributeStructure)(a => a.id.toString == attributeId)
        .toFuture
      actorResponse <- commander(tenantId).askWithStatus(ref =>
        UpdateAttribute(tenantId, attrId, attribute, offsetDateTimeSupplier.get(), ref)
      )
    } yield actorResponse.toAPI

    onComplete(result) { updateTenantAttributeResponse[Tenant](operationLabel)(addTenantAttribute200) }
  }
}
