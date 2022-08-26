package it.pagopa.interop.tenantmanagement.common.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  lazy val queueUrl: String    = config.getString("tenant-management.persistence-events-queue-url")
  lazy val serverPort: Int     = config.getInt("tenant-management.port")
  val jwtAudience: Set[String] = config.getString("tenant-management.jwt.audience").split(",").toSet.filter(_.nonEmpty)

  val numberOfProjectionTags: Int = config.getInt("akka.cluster.sharding.number-of-shards")
  def projectionTag(index: Int)   = s"interop-be-tenant-management-persistence|$index"
  val projectionsEnabled: Boolean = config.getBoolean("akka.projection.enabled")

  require(jwtAudience.nonEmpty, "Audience cannot be empty")
}
