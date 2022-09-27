package it.pagopa.interop.tenantmanagement.model.persistence

import spray.json._
import spray.json.DefaultJsonProtocol._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.queue.message.ProjectableEvent
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantFeature.PersistentCertifier

object TenantEventsSerde {

  val tenantToJson: PartialFunction[ProjectableEvent, JsValue] = {
    case x: TenantCreated          => x.toJson
    case x: TenantUpdated          => x.toJson
    case x: SelfCareMappingCreated => x.toJson
  }

  private val tenantCreated: String          = "tenant-created"
  private val tenantUpdated: String          = "tenant-updated"
  private val selfCareMappingCreated: String = "selfcare-mapping-created"

  val jsonToTenant: PartialFunction[String, JsValue => ProjectableEvent] = {
    case `tenantCreated`          => _.convertTo[TenantCreated]
    case `tenantUpdated`          => _.convertTo[TenantUpdated]
    case `selfCareMappingCreated` => _.convertTo[SelfCareMappingCreated]
  }

  def getKind(e: Event): String = e match {
    case TenantCreated(_)             => tenantCreated
    case TenantUpdated(_)             => tenantUpdated
    case SelfCareMappingCreated(_, _) => selfCareMappingCreated
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

  implicit val pvsFormat: RootJsonFormat[PersistentVerificationRenewal] =
    new RootJsonFormat[PersistentVerificationRenewal] {
      override def read(json: JsValue): PersistentVerificationRenewal = json match {
        case JsString("REVOKE_ON_EXPIRATION") => PersistentVerificationRenewal.REVOKE_ON_EXPIRATION
        case JsString("AUTOMATIC_RENEWAL")    => PersistentVerificationRenewal.AUTOMATIC_RENEWAL
        case x => throw new DeserializationException(s"Unable to deserialize PersistentTenantKind: unmapped kind $x")
      }
      override def write(obj: PersistentVerificationRenewal): JsValue = obj match {
        case PersistentVerificationRenewal.REVOKE_ON_EXPIRATION => JsString("REVOKE_ON_EXPIRATION")
        case PersistentVerificationRenewal.AUTOMATIC_RENEWAL    => JsString("AUTOMATIC_RENEWAL")
      }
    }

  private implicit val pasFormat: RootJsonFormat[PersistentTenantAttribute] = {
    implicit val pcaFormat: RootJsonFormat[PersistentCertifiedAttribute] = jsonFormat3(
      PersistentCertifiedAttribute.apply
    )
    implicit val pdaFormat: RootJsonFormat[PersistentDeclaredAttribute] = jsonFormat3(PersistentDeclaredAttribute.apply)
    implicit val pvaFormat: RootJsonFormat[PersistentVerifiedAttribute] = jsonFormat5(PersistentVerifiedAttribute.apply)

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

  private implicit val pexFormat: RootJsonFormat[PersistentExternalId]    = jsonFormat2(PersistentExternalId.apply)
  private implicit val ptFormat: RootJsonFormat[PersistentTenant]         = jsonFormat7(PersistentTenant.apply)
  private implicit val tcFormat: RootJsonFormat[TenantCreated]            = jsonFormat1(TenantCreated.apply)
  private implicit val tuFormat: RootJsonFormat[TenantUpdated]            = jsonFormat1(TenantUpdated.apply)
  private implicit val scmcFormat: RootJsonFormat[SelfCareMappingCreated] = jsonFormat2(SelfCareMappingCreated.apply)

}
