package it.pagopa.interop.tenantmanagement.model.persistence

import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantFeature.PersistentCertifier
import it.pagopa.interop.tenantmanagement.model.tenant._
import spray.json.DefaultJsonProtocol._
import spray.json._

object JsonFormats {

  implicit val peFormat: RootJsonFormat[PersistentExternalId] = jsonFormat2(PersistentExternalId.apply)

  implicit val ptfcFormat: RootJsonFormat[PersistentCertifier] = jsonFormat1(PersistentCertifier.apply)

  implicit val ptfFormat: RootJsonFormat[PersistentTenantFeature] = new RootJsonFormat[PersistentTenantFeature] {
    def write(obj: PersistentTenantFeature): JsValue = {
      val json: JsValue = obj match {
        case c: PersistentCertifier => c.toJson
      }
      JsObject(json.asJsObject.fields + ("type" -> JsString(obj.productPrefix)))
    }

    def read(json: JsValue): PersistentTenantFeature =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("PersistentCertifier")) => json.convertTo[PersistentCertifier]
        case unrecognized                         =>
          deserializationError(s"PersistentTenantFeature deserialization error ${unrecognized.toString}")
      }
  }

  implicit val pcaFormat: RootJsonFormat[PersistentCertifiedAttribute] = jsonFormat3(PersistentCertifiedAttribute.apply)
  implicit val pdaFormat: RootJsonFormat[PersistentDeclaredAttribute]  = jsonFormat3(PersistentDeclaredAttribute.apply)
  implicit val ptvFormat: RootJsonFormat[PersistentTenantVerifier]     = jsonFormat4(PersistentTenantVerifier.apply)
  implicit val ptrFormat: RootJsonFormat[PersistentTenantRevoker]      = jsonFormat5(PersistentTenantRevoker.apply)
  implicit val pvaFormat: RootJsonFormat[PersistentVerifiedAttribute]  = jsonFormat4(PersistentVerifiedAttribute.apply)

  implicit val ptaFormat: RootJsonFormat[PersistentTenantAttribute] = new RootJsonFormat[PersistentTenantAttribute] {
    def write(obj: PersistentTenantAttribute): JsValue = {
      val json: JsValue = obj match {
        case c: PersistentCertifiedAttribute => c.toJson
        case d: PersistentDeclaredAttribute  => d.toJson
        case v: PersistentVerifiedAttribute  => v.toJson
      }
      JsObject(json.asJsObject.fields + ("type" -> JsString(obj.productPrefix)))
    }

    def read(json: JsValue): PersistentTenantAttribute =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("PersistentCertifiedAttribute")) => json.convertTo[PersistentCertifiedAttribute]
        case Seq(JsString("PersistentDeclaredAttribute"))  => json.convertTo[PersistentDeclaredAttribute]
        case Seq(JsString("PersistentVerifiedAttribute"))  => json.convertTo[PersistentVerifiedAttribute]
        case unrecognized                                  =>
          deserializationError(s"PersistentTenantAttribute deserialization error ${unrecognized.toString}")
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

  private implicit val ptmFormat: RootJsonFormat[PersistentTenantMail] = jsonFormat4(PersistentTenantMail.apply)
  implicit val ptFormat: RootJsonFormat[PersistentTenant]              = jsonFormat10(PersistentTenant.apply)

  implicit val pcFormat: RootJsonFormat[TenantCreated] = jsonFormat1(TenantCreated.apply)
  implicit val puFormat: RootJsonFormat[TenantUpdated] = jsonFormat1(TenantUpdated.apply)
}
