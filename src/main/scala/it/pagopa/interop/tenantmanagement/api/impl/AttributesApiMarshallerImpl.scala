package it.pagopa.interop.tenantmanagement.api.impl

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import it.pagopa.interop.tenantmanagement.api.AttributesApiMarshaller
import it.pagopa.interop.tenantmanagement.model.{Problem, Tenant}
import spray.json._
import it.pagopa.interop.tenantmanagement.model.TenantAttributeSeed

object AttributesApiMarshallerImpl extends AttributesApiMarshaller with SprayJsonSupport with DefaultJsonProtocol {

  override implicit def fromEntityUnmarshallerTenantAttributeSeed: FromEntityUnmarshaller[TenantAttributeSeed] =
    sprayJsonUnmarshaller[TenantAttributeSeed]
  override implicit def toEntityMarshallerProblem: ToEntityMarshaller[Problem] = sprayJsonMarshaller[Problem]
  override implicit def toEntityMarshallerTenant: ToEntityMarshaller[Tenant]   = sprayJsonMarshaller[Tenant]
}
