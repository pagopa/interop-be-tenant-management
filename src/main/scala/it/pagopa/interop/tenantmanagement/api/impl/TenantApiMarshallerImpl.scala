package it.pagopa.interop.tenantmanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.tenantmanagement.api.TenantApiMarshaller
import it.pagopa.interop.tenantmanagement.model.{Problem, Tenant, TenantSeed}
import spray.json._
import it.pagopa.interop.tenantmanagement.model.TenantDelta

object TenantApiMarshallerImpl extends TenantApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def fromEntityUnmarshallerTenantDelta: FromEntityUnmarshaller[TenantDelta] =
    sprayJsonUnmarshaller[TenantDelta]
  override implicit def fromEntityUnmarshallerTenantSeed: FromEntityUnmarshaller[TenantSeed]   =
    sprayJsonUnmarshaller[TenantSeed]
  override implicit def toEntityMarshallerTenant: ToEntityMarshaller[Tenant]   = sprayJsonMarshaller[Tenant]
  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]
}
