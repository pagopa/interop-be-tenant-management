package it.pagopa.interop.tenantmanagement

import akka.actor
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import it.pagopa.interop.commons.utils.AkkaUtils.getShard
import it.pagopa.interop.commons.utils.service.{OffsetDateTimeSupplier, UUIDSupplier}
import it.pagopa.interop.tenantmanagement.api.impl.{
  AttributesApiMarshallerImpl,
  AttributesApiServiceImpl,
  TenantApiMarshallerImpl,
  TenantApiServiceImpl
}
import it.pagopa.interop.tenantmanagement.api.{AttributesApi, AttributesApiMarshaller, TenantApi, TenantApiMarshaller}
import it.pagopa.interop.tenantmanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.tenantmanagement.model.persistence.{
  Command,
  CreateTenant,
  TenantPersistentBehavior,
  UpdateTenant
}
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentTenant, PersistentTenantDelta}
import it.pagopa.interop.tenantmanagement.server.Controller
import it.pagopa.interop.tenantmanagement.server.impl.Dependencies
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertion
import spray.json._

import java.net.InetAddress
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

trait ItSpecHelper
    extends ItSpecConfiguration
    with ItCqrsSpec
    with MockFactory
    with SprayJsonSupport
    with DefaultJsonProtocol
    with Dependencies {
  self: ScalaTestWithActorTestKit =>

  val bearerToken: String                   = "token"
  final val requestHeaders: Seq[HttpHeader] =
    Seq(
      headers.Authorization(OAuth2BearerToken("token")),
      headers.RawHeader("X-Correlation-Id", "test-id"),
      headers.`X-Forwarded-For`(RemoteAddress(InetAddress.getByName("127.0.0.1")))
    )

  val mockUUIDSupplier: UUIDSupplier               = mock[UUIDSupplier]
  val mockDateTimeSupplier: OffsetDateTimeSupplier = mock[OffsetDateTimeSupplier]

  val tenantApiMarshaller: TenantApiMarshaller         = TenantApiMarshallerImpl
  val attributesApiMarshaller: AttributesApiMarshaller = AttributesApiMarshallerImpl

  var controller: Option[Controller]                 = None
  var bindServer: Option[Future[Http.ServerBinding]] = None

  val wrappingDirective: AuthenticationDirective[Seq[(String, String)]] =
    SecurityDirectives.authenticateOAuth2("SecurityRealm", AdminMockAuthenticator)

  val sharding: ClusterSharding                 = ClusterSharding(system)
  def commander(id: UUID): EntityRef[Command]   = commander(id.toString)
  def commander(id: String): EntityRef[Command] =
    sharding.entityRefFor(
      TenantPersistentBehavior.TypeKey,
      getShard(id, ApplicationConfiguration.numberOfProjectionTags)
    )

  val httpSystem: ActorSystem[Any]                        =
    ActorSystem(Behaviors.ignore[Any], name = system.name, config = system.settings.config)
  implicit val executionContext: ExecutionContextExecutor = httpSystem.executionContext
  val classicSystem: actor.ActorSystem                    = httpSystem.classicSystem

  override def startServer(): Unit = {
    val persistentEntity: Entity[Command, ShardingEnvelope[Command]] =
      Entity(TenantPersistentBehavior.TypeKey)(behaviorFactory)

    Cluster(system).manager ! Join(Cluster(system).selfMember.address)
    sharding.init(persistentEntity)

    val attributesApi =
      new AttributesApi(
        new AttributesApiServiceImpl(system, sharding, persistentEntity, mockDateTimeSupplier),
        attributesApiMarshaller,
        wrappingDirective
      )

    val tenantApi =
      new TenantApi(
        new TenantApiServiceImpl(system, sharding, persistentEntity, mockDateTimeSupplier),
        tenantApiMarshaller,
        wrappingDirective
      )

    if (ApplicationConfiguration.projectionsEnabled) initCqrsProjection()

    controller = Some(new Controller(attributes = attributesApi, tenant = tenantApi)(classicSystem))

    controller foreach { controller =>
      bindServer = Some(
        Http()(classicSystem)
          .newServerAt("0.0.0.0", 18088)
          .bind(controller.routes)
      )

      Await.result(bindServer.get, 100.seconds)
    }
  }

  override def shutdownServer(): Unit = {
    bindServer.foreach(_.foreach(_.unbind()))
    ActorTestKit.shutdown(httpSystem, 5.seconds)
  }

  def compareTenants(item1: PersistentTenant, item2: PersistentTenant): Assertion =
    item1 shouldBe item2

  def createTenant(tenant: PersistentTenant): PersistentTenant =
    commander(tenant.id).ask(ref => CreateTenant(tenant, ref)).futureValue.getValue

  def updateTenant(tenantDelta: PersistentTenantDelta): PersistentTenant =
    commander(tenantDelta.id)
      .ask(ref => UpdateTenant(tenantDelta, mockDateTimeSupplier.get(), ref))
      .futureValue
      .getValue

}
