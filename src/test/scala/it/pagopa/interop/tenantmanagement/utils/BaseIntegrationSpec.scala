package it.pagopa.interop.tenantmanagement.utils

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives.SecurityDirectives
import it.pagopa.interop.tenantmanagement.api._
import it.pagopa.interop.tenantmanagement.api.impl._
import it.pagopa.interop.tenantmanagement.model.persistence.TenantPersistentBehavior
import it.pagopa.interop.tenantmanagement.server.Controller
import it.pagopa.interop.tenantmanagement.server.impl.Main.behaviorFactory
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import it.pagopa.interop.commons.utils.{BEARER, USER_ROLES}

import com.typesafe.scalalogging.Logger
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import com.typesafe.config.ConfigFactory
import munit.FunSuite
import akka.cluster.sharding.typed.ShardingEnvelope
import it.pagopa.interop.tenantmanagement.model.persistence.Command
import akka.actor.typed.ActorSystem
import akka.actor
import java.time.OffsetDateTime
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import cats.data.NonEmptyList

abstract class BaseIntegrationSpec extends FunSuite with SpecHelper {

  override def munitFixtures = List(suiteState)

  var mockedTimes: NonEmptyList[OffsetDateTime] = null

  def mockedTime: OffsetDateTime = mockedTimes.head

  val suiteState: Fixture[ActorSystem[_]] =
    new Fixture[ActorSystem[_]]("actorSystem") {
      private var bindServer: Http.ServerBinding = null
      private var actorTestKit: ActorTestKit     = null
      def apply(): ActorSystem[_]                = actorTestKit.internalSystem

      override def beforeEach(context: BeforeEach): Unit = {
        mockedTimes = NonEmptyList.one(OffsetDateTime.now())
      }

      override def beforeAll(): Unit = {
        Logger(this.getClass()) // * A logger should be created before the one in akka to avoid the "replay" message
        actorTestKit = ActorTestKit(ConfigFactory.load())
        Cluster(actorTestKit.internalSystem).manager ! Join(Cluster(actorTestKit.internalSystem).selfMember.address)
        val sharding: ClusterSharding                                    = ClusterSharding(actorTestKit.internalSystem)
        val persistentEntity: Entity[Command, ShardingEnvelope[Command]] =
          Entity(TenantPersistentBehavior.TypeKey)(behaviorFactory)

        sharding.init(persistentEntity)

        val offsetDateTimeSupplier: OffsetDateTimeSupplier = () =>
          mockedTimes match {
            case NonEmptyList(head, Nil)  => head
            case NonEmptyList(head, tail) => mockedTimes = NonEmptyList.fromListUnsafe(tail); head
          }

        val attributesApi: AttributesApi = new AttributesApi(
          new AttributesApiServiceImpl(actorTestKit.internalSystem, sharding, persistentEntity, offsetDateTimeSupplier)(
            actorTestKit.system.executionContext
          ),
          AttributesApiMarshallerImpl,
          SecurityDirectives.authenticateOAuth2(
            "SecurityRealm",
            {
              case Provided(identifier) =>
                Some(Seq(BEARER -> identifier, USER_ROLES -> "admin,security,api,m2m,internal"))
              case Missing              => None
            }
          )
        )

        val tenantApi: TenantApi = new TenantApi(
          new TenantApiServiceImpl(actorTestKit.internalSystem, sharding, persistentEntity, offsetDateTimeSupplier)(
            actorTestKit.system.executionContext
          ),
          TenantApiMarshallerImpl,
          SecurityDirectives.authenticateOAuth2(
            "SecurityRealm",
            {
              case Provided(identifier) =>
                Some(Seq(BEARER -> identifier, USER_ROLES -> "admin,security,api,m2m,internal"))
              case Missing              => None
            }
          )
        )

        implicit val classic: actor.ActorSystem = actorTestKit.internalSystem.classicSystem
        val controller: Controller              = new Controller(attributesApi, tenantApi)

        bindServer = Await.result(
          Http()
            .newServerAt("0.0.0.0", 18088)
            .bind(controller.routes),
          100.seconds
        )
      }

      override def afterAll(): Unit = {
        bindServer.unbind()
        ActorTestKit.shutdown(actorTestKit.internalSystem, 5.seconds)
        super.afterAll()
      }
    }
}
