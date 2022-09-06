package it.pagopa.interop.tenantmanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.commons.utils.SprayCommonFormats._
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.tenantmanagement.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val externalIdFormat: RootJsonFormat[ExternalId]                      = jsonFormat2(ExternalId)
  implicit val verifierFormat: RootJsonFormat[TenantVerifier]                    = jsonFormat4(TenantVerifier)
  implicit val revokerFormat: RootJsonFormat[TenantRevoker]                      = jsonFormat5(TenantRevoker)
  implicit val declaredAttributesFormat: RootJsonFormat[DeclaredTenantAttribute] = jsonFormat3(DeclaredTenantAttribute)
  implicit val certifiedAttributesFormat: RootJsonFormat[CertifiedTenantAttribute] = jsonFormat3(
    CertifiedTenantAttribute
  )
  implicit val verifiedAttributesFormat: RootJsonFormat[VerifiedTenantAttribute] = jsonFormat5(VerifiedTenantAttribute)
  implicit val attributesFormat: RootJsonFormat[TenantAttribute]                 = jsonFormat3(TenantAttribute)
  implicit val certifierFormat: RootJsonFormat[Certifier]                        = jsonFormat1(Certifier)
  implicit val tenantFeatureFormat: RootJsonFormat[TenantFeature]                = jsonFormat1(TenantFeature)
  implicit val tenantFormat: RootJsonFormat[Tenant]                              = jsonFormat7(Tenant)
  implicit val tenantDeltaFormat: RootJsonFormat[TenantDelta]                    = jsonFormat2(TenantDelta)
  implicit val tenantSeedFormat: RootJsonFormat[TenantSeed]                      = jsonFormat4(TenantSeed)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError]                  = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]                            = jsonFormat5(Problem)

  final val entityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]

  final val serviceErrorCodePrefix: String = "018"
  final val defaultProblemType: String     = "about:blank"
  final val defaultErrorMessage: String    = "Unknown error"

  def problemOf(httpError: StatusCode, error: ComponentError): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )

  def problemOf(httpError: StatusCode, errors: List[ComponentError]): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = errors.map(error =>
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultErrorMessage)
        )
      )
    )
}
