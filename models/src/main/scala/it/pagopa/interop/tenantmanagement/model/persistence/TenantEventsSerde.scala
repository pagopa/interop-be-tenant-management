package it.pagopa.interop.tenantmanagement.model.persistence

import spray.json._
import spray.json.DefaultJsonProtocol._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.queue.message.ProjectableEvent

object TenantEventsSerde {

  val tenantToJson: PartialFunction[ProjectableEvent, JsValue] = { case x @ TenantCreated(_) =>
    x.toJson

  }

  val jsonToTenant: PartialFunction[String, JsValue => ProjectableEvent] = { case `tenantCreated` =>
    _.convertTo[TenantCreated]

  }

  def getKind(e: Event): String = e match {
    case TenantCreated(_) => tenantCreated

  }

  private val tenantCreated: String = "tenant-created"

  private implicit val pasFormat: RootJsonFormat[PersistentTenantAttribute]  = ???
  private implicit val pexFormat: RootJsonFormat[PersistentTenantExternalId] = ???
//  jsonFormat3(
//    PersistentTenantAttributes.apply
//  )

  private implicit val ptFormat: RootJsonFormat[PersistentTenant] = jsonFormat5(PersistentTenant.apply)

  private implicit val tcFormat: RootJsonFormat[TenantCreated] = jsonFormat1(TenantCreated.apply)

}
