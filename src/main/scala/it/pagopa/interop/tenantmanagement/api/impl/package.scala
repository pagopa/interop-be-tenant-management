package it.pagopa.interop.tenantmanagement.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat
import it.pagopa.interop.commons.utils.errors.ComponentError
import it.pagopa.interop.tenantmanagement.model._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val attributesFormat: RootJsonFormat[TenantAttribute] = jsonFormat3(TenantAttribute)
  implicit val tenantFormat: RootJsonFormat[Tenant]              = jsonFormat3(Tenant)
  implicit val tenantSeedFormat: RootJsonFormat[TenantSeed]      = jsonFormat2(TenantSeed)
  implicit val problemErrorFormat: RootJsonFormat[ProblemError]  = jsonFormat2(ProblemError)
  implicit val problemFormat: RootJsonFormat[Problem]            = jsonFormat5(Problem)

  final val serviceErrorCodePrefix: String = "018"
  final val defaultProblemType: String     = "about:blank"

  def problemOf(httpError: StatusCode, error: ComponentError, defaultMessage: String = "Unknown error"): Problem =
    Problem(
      `type` = defaultProblemType,
      status = httpError.intValue,
      title = httpError.defaultMessage,
      errors = Seq(
        ProblemError(
          code = s"$serviceErrorCodePrefix-${error.code}",
          detail = Option(error.getMessage).getOrElse(defaultMessage)
        )
      )
    )
}
