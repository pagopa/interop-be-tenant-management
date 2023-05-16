package it.pagopa.interop.tenantmanagement.model.persistence.projection

import akka.actor.typed.ActorSystem
import it.pagopa.interop.commons.cqrs.model._
import it.pagopa.interop.commons.cqrs.service.CqrsProjection
import it.pagopa.interop.commons.cqrs.service.DocumentConversions._
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.tenantmanagement.model.persistence._
import org.mongodb.scala.model.{Filters, Updates}
import org.mongodb.scala.{Document, MongoCollection}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import spray.json.enrichAny

import scala.concurrent.ExecutionContext

object TenantCqrsProjection {
  def projection(offsetDbConfig: DatabaseConfig[JdbcProfile], mongoDbConfig: MongoDbConfig, projectionId: String)(
    implicit
    system: ActorSystem[_],
    ec: ExecutionContext
  ): CqrsProjection[Event] =
    CqrsProjection[Event](offsetDbConfig, mongoDbConfig, projectionId = projectionId, eventHandler)

  private def eventHandler(collection: MongoCollection[Document], event: Event): PartialMongoAction = event match {
    case TenantCreated(t)             =>
      ActionWithDocument(collection.insertOne, Document(s"{ data: ${t.toJson.compactPrint} }"))
    case TenantUpdated(t)             =>
      ActionWithBson(collection.updateOne(Filters.eq("data.id", t.id.toString), _), Updates.set("data", t.toDocument))
    case TenantDeleted(tenantId)      =>
      Action(collection.deleteOne(Filters.eq("data.id", tenantId)))
    case SelfcareMappingCreated(_, _) => NoOpAction
    case SelfcareMappingDeleted(_)    => NoOpAction
  }

}
