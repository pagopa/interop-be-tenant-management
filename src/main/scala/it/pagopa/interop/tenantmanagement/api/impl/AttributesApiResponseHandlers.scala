package it.pagopa.interop.tenantmanagement.api.impl

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors.{InvalidAttributeStructure, _}

import scala.util.{Failure, Success, Try}

object AttributesApiResponseHandlers extends AkkaResponses {

  def getTenantAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                     => success(s)
      case Failure(ex: TenantNotFound)    => notFound(ex, logMessage)
      case Failure(ex: AttributeNotFound) => notFound(ex, logMessage)
      case Failure(ex)                    => internalServerError(ex, logMessage)
    }

  def addTenantAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: InvalidAttributeStructure.type) => badRequest(ex, logMessage)
      case Failure(ex: TenantNotFound)                 => notFound(ex, logMessage)
      case Failure(ex: AttributeAlreadyExists)         => conflict(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def updateTenantAttributeResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: InvalidAttributeStructure.type) => badRequest(ex, logMessage)
      case Failure(ex: TenantNotFound)                 => notFound(ex, logMessage)
      case Failure(ex: AttributeNotFound)              => notFound(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }
}
