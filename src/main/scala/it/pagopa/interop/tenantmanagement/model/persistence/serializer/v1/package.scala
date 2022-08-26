package it.pagopa.interop.tenantmanagement.model.persistence.serializer

import cats.implicits.toTraverseOps
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.protobufUtils.{
  toPersistentTenant,
  toProtobufTenant
}
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.state.{StateV1, TenantsV1}

package object v1 {

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] = state =>
    for {
      tenants <- state.tenants.toList
        .traverse(entry => toPersistentTenant(entry.value).map(tenant => (entry.key, tenant)))
    } yield State(tenants.toMap)

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] = state =>
    for {
      tenantsV1 <- state.tenants.toList.traverse { case (key, tenant) =>
        toProtobufTenant(tenant).map(value => TenantsV1(key, value))
      }
    } yield StateV1(tenantsV1)

  implicit def tenantCreatedV1PersistEventDeserializer: PersistEventDeserializer[TenantCreatedV1, TenantCreated] =
    event => toPersistentTenant(event.tenant).map(TenantCreated)

  implicit def tenantCreatedV1PersistEventSerializer: PersistEventSerializer[TenantCreated, TenantCreatedV1] =
    event => toProtobufTenant(event.tenant).map(TenantCreatedV1.of)

  implicit def tenantUpdatedV1PersistEventDeserializer: PersistEventDeserializer[TenantUpdatedV1, TenantUpdated] =
    event => toPersistentTenant(event.tenant).map(TenantUpdated)

  implicit def tenantUpdatedV1PersistEventSerializer: PersistEventSerializer[TenantUpdated, TenantUpdatedV1] =
    event => toProtobufTenant(event.tenant).map(TenantUpdatedV1.of)

}
