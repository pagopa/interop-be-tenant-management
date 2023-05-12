package it.pagopa.interop.tenantmanagement.api.impl

import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.errors.AkkaResponses
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors._

import scala.util.{Failure, Success, Try}

object TenantApiResponseHandlers extends AkkaResponses {

  def createTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                                  => success(s)
      case Failure(ex: InvalidAttributeStructure.type) => badRequest(ex, logMessage)
      case Failure(ex: TenantAlreadyExists)            => conflict(ex, logMessage)
      case Failure(ex)                                 => internalServerError(ex, logMessage)
    }

  def getTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                  => success(s)
      case Failure(ex: TenantNotFound) => notFound(ex, logMessage)
      case Failure(ex)                 => internalServerError(ex, logMessage)
    }

  def getTenantByExternalIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: TenantByExternalIdNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def updateTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                  => success(s)
      case Failure(ex: TenantNotFound) => notFound(ex, logMessage)
      case Failure(ex)                 => internalServerError(ex, logMessage)
    }

  def getTenantBySelfcareIdResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                              => success(s)
      case Failure(ex: TenantBySelfcareIdNotFound) => notFound(ex, logMessage)
      case Failure(ex)                             => internalServerError(ex, logMessage)
    }

  def deleteTenantResponse[T](logMessage: String)(
    success: T => Route
  )(result: Try[T])(implicit contexts: Seq[(String, String)], logger: LoggerTakingImplicit[ContextFieldsToLog]): Route =
    result match {
      case Success(s)                  => success(s)
      case Failure(ex: TenantNotFound) => notFound(ex, logMessage)
      case Failure(ex)                 => internalServerError(ex, logMessage)
    }

}
