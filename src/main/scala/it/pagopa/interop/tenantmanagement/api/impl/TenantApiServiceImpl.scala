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
import it.pagopa.interop.tenantmanagement.api.TenantApiService
import it.pagopa.interop.tenantmanagement.api.impl.TenantApiResponseHandlers._
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors._
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.tenantmanagement.model.persistence.Adapters._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.tenant._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class TenantApiServiceImpl(
  system: ActorSystem[_],
  sharding: ClusterSharding,
  entity: Entity[Command, ShardingEnvelope[Command]],
  offsetDateTimeSupplier: OffsetDateTimeSupplier
)(implicit ec: ExecutionContext)
    extends TenantApiService {

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)
  private implicit val timeout: Timeout                                 = Timeout(300.seconds)
  private val settings: ClusterShardingSettings = entity.settings.getOrElse(ClusterShardingSettings(system))

  private def commanderForTenantId(tenantId: String): EntityRef[Command] =
    sharding.entityRefFor(TenantPersistentBehavior.TypeKey, getShard(tenantId, settings.numberOfShards))

  private def commanderForSelfcareId(selfcareId: String): EntityRef[Command] =
    sharding.entityRefFor(TenantPersistentBehavior.TypeKey, getShard(selfcareId, settings.numberOfShards))

  private def commanders: List[EntityRef[Command]] = (0 until settings.numberOfShards)
    .map(_.toString)
    .toList
    .map(sharding.entityRefFor(TenantPersistentBehavior.TypeKey, _))

  private def addMapping(selfcareId: String, tenantUUID: UUID): Future[Unit] =
    commanderForSelfcareId(selfcareId).askWithStatus(AddSelfcareIdTenantMapping(selfcareId, tenantUUID, _))

  private def getTenantIdBySelfcareId(selfcareId: String): Future[UUID] =
    commanderForSelfcareId(selfcareId).askWithStatus(GetTenantBySelfcareId(selfcareId, _))

  override def createTenant(tenantSeed: TenantSeed)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel =
      s"Creating tenant with externalId (${tenantSeed.externalId.origin},${tenantSeed.externalId.value})"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      tenant        <- PersistentTenant.fromAPI(tenantSeed, offsetDateTimeSupplier).toFuture
      actorResponse <- commanderForTenantId(tenant.id.toString).askWithStatus(CreateTenant(tenant, _))
    } yield actorResponse.toAPI

    onComplete(result) { createTenantResponse[Tenant](operationLabel)(createTenant200) }
  }

  override def getTenant(tenantId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving tenant $tenantId"
    logger.info(operationLabel)

    val result: Future[Tenant] =
      commanderForTenantId(tenantId).askWithStatus(ref => GetTenant(tenantId, ref)).map(_.toAPI)

    onComplete(result) { getTenantResponse[Tenant](operationLabel)(getTenant200) }
  }

  override def getTenantByExternalId(origin: String, code: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving tenant by External Id Origin $origin Code $code"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      maybeTenant <- findFirstTenantWithExternalId(PersistentExternalId.fromAPI(ExternalId(origin, code)))
      tenant      <- maybeTenant.toFuture(TenantByExternalIdNotFound(origin, code))
    } yield tenant.toAPI

    onComplete(result) { getTenantByExternalIdResponse[Tenant](operationLabel)(getTenantByExternalId200) }
  }

  def findFirstTenantWithExternalId(externalId: PersistentExternalId): Future[Option[PersistentTenant]] = {
    def askAction(commanderForTenantId: EntityRef[Command]): Future[List[PersistentTenant]] =
      commanderForTenantId.askWithStatus[List[PersistentTenant]](r => GetTenantsWithExternalId(externalId, r))

    Future.traverse(commanders)(askAction).map(_.flatten).flatMap {
      case Nil           => Future.successful(None)
      case tenant :: Nil => Future.successful(tenant.some)
      case ts => Future.failed(MultipleTenantsForExternalId(externalId.origin, externalId.value, ts.map(_.id)))
    }
  }

  override def updateTenant(tenantId: String, tenantDelta: TenantDelta)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Updating Tenant $tenantId"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      tenantUUID <- tenantId.toFutureUUID
      tenant     <- commanderForTenantId(tenantId).askWithStatus(r => GetTenant(tenantId, r))
      delta      <- PersistentTenantDelta.fromAPI(tenant, tenantDelta, offsetDateTimeSupplier).toFuture
      result <- commanderForTenantId(tenantId).askWithStatus(r => UpdateTenant(delta, offsetDateTimeSupplier.get(), r))
      _      <- delta.selfcareId.fold(Future.unit)(addMapping(_, tenantUUID))
    } yield result.toAPI

    onComplete(result) { updateTenantResponse[Tenant](operationLabel)(updateTenant200) }
  }

  override def getTenantBySelfcareId(selfcareId: String)(implicit
    toEntityMarshallerProblem: ToEntityMarshaller[Problem],
    toEntityMarshallerTenant: ToEntityMarshaller[Tenant],
    contexts: Seq[(String, String)]
  ): Route = {
    val operationLabel = s"Retrieving Tenant by Selfcare Id $selfcareId"
    logger.info(operationLabel)

    val result: Future[Tenant] = for {
      tenantId <- getTenantIdBySelfcareId(selfcareId).map(_.toString())
      response <- commanderForTenantId(tenantId).askWithStatus(ref => GetTenant(tenantId, ref))
    } yield response.toAPI

    onComplete(result) { getTenantBySelfcareIdResponse[Tenant](operationLabel)(getTenantBySelfcareId200) }
  }

  override def deleteTenant(
    tenantId: String
  )(implicit toEntityMarshallerProblem: ToEntityMarshaller[Problem], contexts: Seq[(String, String)]): Route = {
    val operationLabel = s"Deleting Tenant $tenantId"
    logger.info(operationLabel)

    val result: Future[Unit] = for {
      tenant <- commanderForTenantId(tenantId).askWithStatus(ref => GetTenant(tenantId, ref))
      _      <- commanderForTenantId(tenantId).askWithStatus[Unit](DeleteTenant(tenantId, _))
      _      <- tenant.selfcareId.fold(Future.unit)(selfcareId =>
        commanderForSelfcareId(selfcareId).askWithStatus[Unit](DeleteSelfcareIdTenantMapping(selfcareId, _))
      )
    } yield ()

    onComplete(result) { deleteTenantResponse[Unit](operationLabel)(_ => deleteTenant204) }
  }

}
