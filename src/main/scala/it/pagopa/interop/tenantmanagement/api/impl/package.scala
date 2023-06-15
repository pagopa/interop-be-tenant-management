package it.pagopa.interop.tenantmanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.utils.errors.ServiceCode
import it.pagopa.interop.tenantmanagement.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val externalIdFormat: RootJsonFormat[ExternalId]                      = jsonFormat2(ExternalId)
  implicit val verifierFormat: RootJsonFormat[TenantVerifier]                    = jsonFormat4(TenantVerifier)
  implicit val revokerFormat: RootJsonFormat[TenantRevoker]                      = jsonFormat5(TenantRevoker)
  implicit val declaredAttributesFormat: RootJsonFormat[DeclaredTenantAttribute] = jsonFormat3(DeclaredTenantAttribute)
  implicit val certifiedAttributesFormat: RootJsonFormat[CertifiedTenantAttribute] =
    jsonFormat3(CertifiedTenantAttribute)
  implicit val verifiedAttributesFormat: RootJsonFormat[VerifiedTenantAttribute] = jsonFormat4(VerifiedTenantAttribute)
  implicit val attributesFormat: RootJsonFormat[TenantAttribute]                 = jsonFormat3(TenantAttribute)
  implicit val certifierFormat: RootJsonFormat[Certifier]                        = jsonFormat1(Certifier)
  implicit val tenantFeatureFormat: RootJsonFormat[TenantFeature]                = jsonFormat1(TenantFeature)
  implicit val mailSeedFormat: RootJsonFormat[MailSeed]                          = jsonFormat3(MailSeed)
  implicit val mailFormat: RootJsonFormat[Mail]                                  = jsonFormat4(Mail)
  implicit val tenantFormat: RootJsonFormat[Tenant]                              = jsonFormat10(Tenant)
  implicit val tenantDeltaFormat: RootJsonFormat[TenantDelta]                    = jsonFormat4(TenantDelta)
  implicit val tenantSeedFormat: RootJsonFormat[TenantSeed]                      = jsonFormat6(TenantSeed)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError]                  = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                            = jsonFormat6(Problem)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  implicit val serviceCode: ServiceCode = ServiceCode("018")

}
