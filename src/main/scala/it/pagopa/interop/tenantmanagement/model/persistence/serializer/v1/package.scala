package it.pagopa.interop.tenantmanagement.model.persistence.serializer

import cats.implicits._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.protobufUtils.{
  toPersistentTenant,
  toProtobufTenant
}
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.state.{StateV1, TenantsV1, TenantMappingV1}
import java.util.UUID
import scala.util.Try

package object v1 {

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] = state =>
    for {
      tenants          <- state.tenants.toList
        .traverse(entry => toPersistentTenant(entry.value).map(tenant => (entry.key, tenant)))
      selfcareMappings <- state.selcareMappings.traverse(tm =>
        Try(UUID.fromString(tm.tenantId)).toEither.map((tm.selfcareId, _))
      )
    } yield State(tenants.toMap, selfcareMappings.toMap)

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] = state =>
    for {
      tenantsV1 <- state.tenants.toList.traverse { case (key, tenant) =>
        toProtobufTenant(tenant).map(value => TenantsV1(key.toString(), value))
      }
      tenantMappinsV1 = state.selfcareMappings.toList.map { case (k, v) => TenantMappingV1(k, v.toString()) }
    } yield StateV1(tenantsV1, tenantMappinsV1)

  implicit def tenantCreatedV1PersistEventDeserializer: PersistEventDeserializer[TenantCreatedV1, TenantCreated] =
    event => toPersistentTenant(event.tenant).map(TenantCreated)

  implicit def tenantCreatedV1PersistEventSerializer: PersistEventSerializer[TenantCreated, TenantCreatedV1] =
    event => toProtobufTenant(event.tenant).map(TenantCreatedV1.of)

  implicit def tenantUpdatedV1PersistEventDeserializer: PersistEventDeserializer[TenantUpdatedV1, TenantUpdated] =
    event => toPersistentTenant(event.tenant).map(TenantUpdated)

  implicit def tenantUpdatedV1PersistEventSerializer: PersistEventSerializer[TenantUpdated, TenantUpdatedV1] =
    event => toProtobufTenant(event.tenant).map(TenantUpdatedV1.of)

  implicit def tenantDeletedV1PersistEventSerializer: PersistEventSerializer[TenantDeleted, TenantDeletedV1] =
    event => TenantDeletedV1.of(event.tenantId).asRight[Throwable]

  implicit def tenantDeletedV1PersistEventDeserializer: PersistEventDeserializer[TenantDeletedV1, TenantDeleted] =
    event => TenantDeleted(event.tenantId).asRight[Throwable]

  implicit def selfcareMappingCreatedV1PersistEventDeserializer
    : PersistEventDeserializer[SelfcareMappingCreatedV1, SelfcareMappingCreated] =
    event => Try(UUID.fromString(event.tenantId)).toEither.map(uuid => SelfcareMappingCreated(event.selfcareId, uuid))

  implicit def selfcareMappingCreatedV1PersistEventSerializer
    : PersistEventSerializer[SelfcareMappingCreated, SelfcareMappingCreatedV1] =
    event => SelfcareMappingCreatedV1(event.selfcareId, event.tenantId.toString()).asRight[Throwable]

  implicit def selfcareMappingDeletedV1PersistEventSerializer
    : PersistEventSerializer[SelfcareMappingDeleted, SelfcareMappingDeletedV1] =
    event => SelfcareMappingDeletedV1.of(event.selfcareId).asRight[Throwable]

  implicit def selfcareMappingDeletedV1PersistEventDeserializer
    : PersistEventDeserializer[SelfcareMappingDeletedV1, SelfcareMappingDeleted] =
    event => SelfcareMappingDeleted(event.selfcareId).asRight[Throwable]

}
