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

  private val tenantCreated: String = "tenant-created"

  val jsonToTenant: PartialFunction[String, JsValue => ProjectableEvent] = { case `tenantCreated` =>
    _.convertTo[TenantCreated]
  }

  def getKind(e: Event): String = e match { case TenantCreated(_) => tenantCreated }

  private implicit val pasFormat: RootJsonFormat[PersistentTenantAttribute] = {
    val pcaFormat: RootJsonFormat[PersistentCertifiedAttribute] = jsonFormat3(PersistentCertifiedAttribute.apply)
    val pdaFormat: RootJsonFormat[PersistentDeclaredAttribute]  = jsonFormat3(PersistentDeclaredAttribute.apply)
    val pvaFormat: RootJsonFormat[PersistentVerifiedAttribute]  = jsonFormat5(PersistentVerifiedAttribute.apply)

    new RootJsonFormat[PersistentTenantAttribute] {
      override def read(json: JsValue): PersistentTenantAttribute = {
        val fields: Map[String, JsValue] = json.asJsObject.fields
        val kind: String                 = fields
          .get("kind")
          .collect { case JsString(kind) => kind }
          .getOrElse(
            throw new DeserializationException(
              "Unable to deserialize PersistentTenantAttribute: missing attribute kind"
            )
          )
        val restOfTheFields: JsObject    = JsObject(fields - "kind")

        kind match {
          case "certified" => restOfTheFields.convertTo[PersistentCertifiedAttribute](pcaFormat)
          case "declared"  => restOfTheFields.convertTo[PersistentDeclaredAttribute](pdaFormat)
          case "verified"  => restOfTheFields.convertTo[PersistentVerifiedAttribute](pvaFormat)
          case x           =>
            throw new DeserializationException(s"Unable to deserialize PersistentTenantAttribute: unmapped kind $x")
        }
      }

      override def write(obj: PersistentTenantAttribute): JsValue = obj match {
        case x: PersistentCertifiedAttribute =>
          JsObject(pcaFormat.write(x).asJsObject.fields + ("kind" -> JsString("certified")))
        case x: PersistentDeclaredAttribute  =>
          JsObject(pdaFormat.write(x).asJsObject.fields + ("kind" -> JsString("declared")))
        case x: PersistentVerifiedAttribute  =>
          JsObject(pvaFormat.write(x).asJsObject.fields + ("kind" -> JsString("verified")))
      }
    }
  }

  private implicit val pexFormat: RootJsonFormat[PersistentTenantExternalId] = jsonFormat2(
    PersistentTenantExternalId.apply
  )

  implicit val ptkFormat: RootJsonFormat[PersistentTenantKind] =
    new RootJsonFormat[PersistentTenantKind] {
      override def read(json: JsValue): PersistentTenantKind = json match {
        case JsString("Standard")  => PersistentTenantKind.STANDARD
        case JsString("Certifier") => PersistentTenantKind.CERTIFIER
        case x => throw new DeserializationException(s"Unable to deserialize PersistentTenantKind: unmapped kind $x")
      }
      override def write(obj: PersistentTenantKind): JsValue = obj match {
        case PersistentTenantKind.STANDARD  => JsString("Standard")
        case PersistentTenantKind.CERTIFIER => JsString("Certifier")
      }
    }

  private implicit val ptFormat: RootJsonFormat[PersistentTenant] = jsonFormat7(PersistentTenant.apply)
  private implicit val tcFormat: RootJsonFormat[TenantCreated]    = jsonFormat1(TenantCreated.apply)

}
