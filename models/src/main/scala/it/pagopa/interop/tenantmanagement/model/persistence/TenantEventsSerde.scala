package it.pagopa.interop.tenantmanagement.model.persistence

import spray.json._
import spray.json.DefaultJsonProtocol._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantFeature.PersistentCertifier

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantMailKind
object TenantEventsSerde {

  val tenantToJson: PartialFunction[ProjectableEvent, JsValue] = {
    case x: TenantCreated          => x.toJson
    case x: TenantUpdated          => x.toJson
    case x: SelfcareMappingCreated => x.toJson
    case x: SelfcareMappingDeleted => x.toJson
  }

  private val tenantCreated: String          = "tenant-created"
  private val tenantUpdated: String          = "tenant-updated"
  private val tenantDeleted: String          = "tenant-deleted"
  private val selfcareMappingCreated: String = "selfcare-mapping-created"
  private val selfcareMappingDeleted: String = "selfcare-mapping-deleted"

  val jsonToTenant: PartialFunction[String, JsValue => ProjectableEvent] = {
    case `tenantCreated`          => _.convertTo[TenantCreated]
    case `tenantUpdated`          => _.convertTo[TenantUpdated]
    case `tenantDeleted`          => _.convertTo[TenantDeleted]
    case `selfcareMappingCreated` => _.convertTo[SelfcareMappingCreated]
    case `selfcareMappingDeleted` => _.convertTo[SelfcareMappingDeleted]
  }

  def getKind(e: Event): String = e match {
    case TenantCreated(_)             => tenantCreated
    case TenantUpdated(_)             => tenantUpdated
    case TenantDeleted(_)             => tenantDeleted
    case SelfcareMappingCreated(_, _) => selfcareMappingCreated
    case SelfcareMappingDeleted(_)    => selfcareMappingDeleted
  }

  // Serdes

  implicit val ptvFormat: RootJsonFormat[PersistentTenantVerifier] = jsonFormat4(PersistentTenantVerifier.apply)
  implicit val ptrFormat: RootJsonFormat[PersistentTenantRevoker]  = jsonFormat5(PersistentTenantRevoker.apply)

  implicit val ptfFormat: RootJsonFormat[PersistentTenantFeature] = new RootJsonFormat[PersistentTenantFeature] {

    implicit val pcFormat: RootJsonFormat[PersistentTenantFeature.PersistentCertifier] = jsonFormat1(
      PersistentTenantFeature.PersistentCertifier.apply
    )

    override def read(json: JsValue): PersistentTenantFeature = {
      val fields: Map[String, JsValue] = json.asJsObject.fields
      val kind: String                 = fields
        .get("kind")
        .collect { case JsString(kind) => kind }
        .getOrElse(
          throw new DeserializationException("Unable to deserialize PersistentTenantFeature: missing attribute kind")
        )
      val restOfTheFields: JsObject    = JsObject(fields - "kind")

      kind match {
        case "certifier" => restOfTheFields.convertTo[PersistentCertifier]
        case x           =>
          throw new DeserializationException(s"Unable to deserialize PersistentTenantFeature: unmapped kind $x")
      }
    }

    override def write(obj: PersistentTenantFeature): JsValue = obj match {
      case x: PersistentCertifier => JsObject(x.toJson.asJsObject.fields + ("kind" -> JsString("certifier")))
    }
  }

  private implicit val pasFormat: RootJsonFormat[PersistentTenantAttribute] = {
    implicit val pcaFormat: RootJsonFormat[PersistentCertifiedAttribute] = jsonFormat3(
      PersistentCertifiedAttribute.apply
    )
    implicit val pdaFormat: RootJsonFormat[PersistentDeclaredAttribute] = jsonFormat3(PersistentDeclaredAttribute.apply)
    implicit val pvaFormat: RootJsonFormat[PersistentVerifiedAttribute] = jsonFormat4(PersistentVerifiedAttribute.apply)

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
          case "CERTIFIED" => restOfTheFields.convertTo[PersistentCertifiedAttribute]
          case "DECLARED"  => restOfTheFields.convertTo[PersistentDeclaredAttribute]
          case "VERIFIED"  => restOfTheFields.convertTo[PersistentVerifiedAttribute]
          case x           =>
            throw new DeserializationException(s"Unable to deserialize PersistentTenantAttribute: unmapped kind $x")
        }
      }

      override def write(obj: PersistentTenantAttribute): JsValue = obj match {
        case x: PersistentCertifiedAttribute =>
          JsObject(pcaFormat.write(x).asJsObject.fields + ("kind" -> JsString("CERTIFIED")))
        case x: PersistentDeclaredAttribute  =>
          JsObject(pdaFormat.write(x).asJsObject.fields + ("kind" -> JsString("DECLARED")))
        case x: PersistentVerifiedAttribute  =>
          JsObject(pvaFormat.write(x).asJsObject.fields + ("kind" -> JsString("VERIFIED")))
      }
    }
  }

  private implicit val ptmkFormat: RootJsonFormat[PersistentTenantMailKind] =
    new RootJsonFormat[PersistentTenantMailKind] {
      override def read(json: JsValue): PersistentTenantMailKind = json match {
        case JsString("CONTACT_EMAIL") => PersistentTenantMailKind.ContactEmail
        case x => throw new DeserializationException(s"Unable to deserialize PersistentTenantKind: unmapped kind $x")
      }
      override def write(obj: PersistentTenantMailKind): JsValue = obj match {
        case PersistentTenantMailKind.ContactEmail => JsString("CONTACT_EMAIL")
      }
    }

  private implicit val ptkFormat: RootJsonFormat[PersistentTenantKind] =
    new RootJsonFormat[PersistentTenantKind] {
      override def read(json: JsValue): PersistentTenantKind = json match {
        case JsString("PA")      => PersistentTenantKind.PA
        case JsString("GSP")     => PersistentTenantKind.GSP
        case JsString("PRIVATE") => PersistentTenantKind.PRIVATE
        case x => throw new DeserializationException(s"Unable to deserialize PersistentTenantKind: unmapped kind $x")
      }
      override def write(obj: PersistentTenantKind): JsValue = obj match {
        case PersistentTenantKind.PA      => JsString("PA")
        case PersistentTenantKind.GSP     => JsString("GSP")
        case PersistentTenantKind.PRIVATE => JsString("PRIVATE")
      }
    }

  private implicit val ptmFormat: RootJsonFormat[PersistentTenantMail]    = jsonFormat4(PersistentTenantMail.apply)
  private implicit val pexFormat: RootJsonFormat[PersistentExternalId]    = jsonFormat2(PersistentExternalId.apply)
  private implicit val ptFormat: RootJsonFormat[PersistentTenant]         = jsonFormat10(PersistentTenant.apply)
  private implicit val tcFormat: RootJsonFormat[TenantCreated]            = jsonFormat1(TenantCreated.apply)
  private implicit val tuFormat: RootJsonFormat[TenantUpdated]            = jsonFormat1(TenantUpdated.apply)
  private implicit val tdFormat: RootJsonFormat[TenantDeleted]            = jsonFormat1(TenantDeleted.apply)
  private implicit val scmcFormat: RootJsonFormat[SelfcareMappingCreated] = jsonFormat2(SelfcareMappingCreated.apply)
  private implicit val scmdFormat: RootJsonFormat[SelfcareMappingDeleted] = jsonFormat1(SelfcareMappingDeleted.apply)

}
