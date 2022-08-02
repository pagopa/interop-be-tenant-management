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
import it.pagopa.interop.commons.queue.QueueWriter
import it.pagopa.interop.commons.utils.OpenapiUtils
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ValidationRequestError
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.commons.utils.service.impl.UUIDSupplierImpl
import it.pagopa.interop.tenantmanagement.api.TenantApi
import it.pagopa.interop.tenantmanagement.api.impl.{TenantApiMarshallerImpl, TenantApiServiceImpl, problemOf}
import it.pagopa.interop.tenantmanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.tenantmanagement.common.system.ApplicationConfiguration.{numberOfProjectionTags, projectionTag}
import it.pagopa.interop.tenantmanagement.model.persistence.{
  Command,
  TenantEventsSerde,
  TenantPersistentBehavior,
  TenantPersistentProjection
}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait Dependencies {

  val uuidSupplier: UUIDSupplier = new UUIDSupplierImpl

  def behaviorFactory(): EntityContext[Command] => Behavior[Command] =
    entityContext =>
      TenantPersistentBehavior(
        entityContext.shard,
        PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
        projectionTag(math.abs(entityContext.entityId.hashCode % numberOfProjectionTags))
      )

  val tenantPersistenceEntity: Entity[Command, ShardingEnvelope[Command]] =
    Entity(TenantPersistentBehavior.TypeKey)(behaviorFactory())

  def initProjections(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Unit = {
    val queueWriter: QueueWriter =
      QueueWriter.get(ApplicationConfiguration.queueUrl)(TenantEventsSerde.tenantToJson)(blockingEc)

    val dbConfig: DatabaseConfig[JdbcProfile] =
      DatabaseConfig.forConfig("akka-persistence-jdbc.shared-databases.slick")

    val tenantPersistentProjection = new TenantPersistentProjection(dbConfig, queueWriter)

    ShardedDaemonProcess(actorSystem).init[ProjectionBehavior.Command](
      name = "tenant-projections",
      numberOfInstances = numberOfProjectionTags,
      behaviorFactory = (i: Int) => ProjectionBehavior(tenantPersistentProjection.projection(projectionTag(i))),
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
      problemOf(StatusCodes.BadRequest, ValidationRequestError(OpenapiUtils.errorFromRequestValidationReport(report)))
    complete(error.status, error)(TenantApiMarshallerImpl.toEntityMarshallerProblem)
  }

  def tenantApi(sharding: ClusterSharding, jwtReader: JWTReader)(implicit actorSystem: ActorSystem[_]) =
    new TenantApi(
      TenantApiServiceImpl(actorSystem, sharding, tenantPersistenceEntity),
      TenantApiMarshallerImpl,
      jwtReader.OAuth2JWTValidatorAsContexts
    )

}
