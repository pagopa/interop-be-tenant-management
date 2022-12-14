package it.pagopa.interop.tenantmanagement.server.impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.ClusterEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Subscribe}
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import buildinfo.BuildInfo
import cats.syntax.all._
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.renderBuildInfo
import it.pagopa.interop.tenantmanagement.common.system.ApplicationConfiguration
import it.pagopa.interop.tenantmanagement.server.Controller

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with Dependencies {

  val logger: Logger = Logger(this.getClass())

  ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      implicit val actorSystem: ActorSystem[Nothing]          = context.system
      implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

      AkkaManagement.get(actorSystem).start()

      val sharding: ClusterSharding = ClusterSharding(context.system)
      sharding.init(tenantPersistenceEntity)

      val cluster: Cluster = Cluster(context.system)
      ClusterBootstrap.get(actorSystem).start()

      val listener: ActorRef[ClusterEvent.MemberEvent] = context.spawn(
        Behaviors.receive[ClusterEvent.MemberEvent]((ctx, event) => {
          ctx.log.info("MemberEvent: {}", event)
          Behaviors.same
        }),
        "listener"
      )

      cluster.subscriptions ! Subscribe(listener, classOf[ClusterEvent.MemberEvent])

      if (ApplicationConfiguration.projectionsEnabled) initProjections()

      logger.info(renderBuildInfo(BuildInfo))
      logger.info(s"Started cluster at ${cluster.selfMember.address}")

      val serverBinding: Future[Http.ServerBinding] = for {
        jwtReader <- getJwtValidator()
        tApi       = tenantApi(sharding, jwtReader)
        aApi       = attributesApi(sharding, jwtReader)
        controller = new Controller(aApi, tApi, validationExceptionToRoute.some)(actorSystem.classicSystem)
        binding <- Http().newServerAt("0.0.0.0", ApplicationConfiguration.serverPort).bind(controller.routes)
      } yield binding

      serverBinding.onComplete {
        case Success(b) =>
          logger.info(s"Started server at ${b.localAddress.getHostString()}:${b.localAddress.getPort()}")
        case Failure(e) =>
          actorSystem.terminate()
          logger.error("Startup error: ", e)
      }

      Behaviors.empty
    },
    BuildInfo.name
  )
}
