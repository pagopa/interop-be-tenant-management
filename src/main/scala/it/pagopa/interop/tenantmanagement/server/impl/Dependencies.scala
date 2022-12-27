package it.pagopa.interop.tenantmanagement.server.impl

import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, ShardedDaemonProcess}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.persistence.typed.PersistenceId
import akka.projection.ProjectionBehavior
import com.atlassian.oai.validator.report.ValidationReport
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import it.pagopa.interop.commons.jwt.service.JWTReader
import it.pagopa.interop.commons.jwt.service.impl.{DefaultJWTReader, getClaimsVerifier}
import it.pagopa.interop.commons.jwt.{JWTConfiguration, KID, PublicKeysHolder, SerializedKey}
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.api.impl._
import it.pagopa.interop.tenantmanagement.api.{AttributesApi, TenantApi}
import it.pagopa.interop.tenantmanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.tenantmanagement.model.persistence.projection._
import it.pagopa.interop.tenantmanagement.model.persistence._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import it.pagopa.interop.commons.cqrs.model.MongoDbConfig
import it.pagopa.interop.commons.utils.errors.{Problem => CommonProblem}
import it.pagopa.interop.tenantmanagement.api.impl.serviceCode

trait Dependencies {

  implicit val loggerTI: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog]("OAuth2JWTValidatorAsContexts")

  val uuidSupplier: UUIDSupplier = UUIDSupplier

  val behaviorFactory: EntityContext[Command] => Behavior[Command] = entityContext =>
    TenantPersistentBehavior(
      entityContext.shard,
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
      ApplicationConfiguration.projectionTag(
        math.abs(entityContext.entityId.hashCode % ApplicationConfiguration.numberOfProjectionTags)
      )
    )

  val tenantPersistenceEntity: Entity[Command, ShardingEnvelope[Command]] =
    Entity(TenantPersistentBehavior.TypeKey)(behaviorFactory)

  def initProjections()(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Unit = initCqrsProjection()

  def initCqrsProjection()(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Unit = {
    val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("akka-persistence-jdbc.shared-databases.slick")
    val mongoDbConfig: MongoDbConfig          = ApplicationConfiguration.mongoDb
    val cqrsProjectionId: String              = "tenant-cqrs-projections"
    val tenantCqrsProjection = TenantCqrsProjection.projection(dbConfig, mongoDbConfig, cqrsProjectionId)

    ShardedDaemonProcess(actorSystem).init[ProjectionBehavior.Command](
      name = cqrsProjectionId,
      numberOfInstances = ApplicationConfiguration.numberOfProjectionTags,
      behaviorFactory =
        (i: Int) => ProjectionBehavior(tenantCqrsProjection.projection(ApplicationConfiguration.projectionTag(i))),
      stopMessage = ProjectionBehavior.Stop
    )
  }

  def getJwtValidator(): Future[JWTReader] = JWTConfiguration.jwtReader
    .loadKeyset()
    .map(keyset =>
      new DefaultJWTReader with PublicKeysHolder {
        var publicKeyset: Map[KID, SerializedKey]                                        = keyset
        override protected val claimsVerifier: DefaultJWTClaimsVerifier[SecurityContext] =
          getClaimsVerifier(audience = ApplicationConfiguration.jwtAudience)
      }
    )
    .toFuture

  val validationExceptionToRoute: ValidationReport => Route = report => {
    val error =
      CommonProblem(StatusCodes.BadRequest, OpenapiUtils.errorFromRequestValidationReport(report), serviceCode, None)
    complete(error.status, error)
  }

  def tenantApi(sharding: ClusterSharding, jwtReader: JWTReader)(implicit
    actorSystem: ActorSystem[_],
    ec: ExecutionContext
  ) = new TenantApi(
    new TenantApiServiceImpl(actorSystem, sharding, tenantPersistenceEntity, OffsetDateTimeSupplier),
    TenantApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

  def attributesApi(sharding: ClusterSharding, jwtReader: JWTReader)(implicit
    actorSystem: ActorSystem[_],
    ec: ExecutionContext
  ) = new AttributesApi(
    new AttributesApiServiceImpl(actorSystem, sharding, tenantPersistenceEntity, OffsetDateTimeSupplier),
    AttributesApiMarshallerImpl,
    jwtReader.OAuth2JWTValidatorAsContexts
  )

}
