package it.pagopa.interop.tenantmanagement.model.persistence.serializer

import cats.implicits.toTraverseOps
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.protobufUtils.{
  toPersistentTenant,
  toProtobufTenant
}
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.state.{StateV1, TenantsV1}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

package object v1 {

  // type alias for traverse type inference
  type ThrowableOr[A] = Either[Throwable, A]

  implicit def stateV1PersistEventDeserializer: PersistEventDeserializer[StateV1, State] =
    state => {
      for {
        tenants <- state.tenants
          .traverse[ThrowableOr, (String, PersistentTenant)](entry =>
            toPersistentTenant(entry.value).map(tenant => (entry.key, tenant))
          )
          .map(_.toMap)
      } yield State(tenants)
    }

  implicit def stateV1PersistEventSerializer: PersistEventSerializer[State, StateV1] =
    state => {
      for {
        tenantsV1 <- state.tenants.toSeq.traverse[ThrowableOr, TenantsV1] { case (key, tenant) =>
          toProtobufTenant(tenant).map(value => TenantsV1(key, value))
        }
      } yield StateV1(tenantsV1)
    }

  implicit def tenantCreatedV1PersistEventDeserializer: PersistEventDeserializer[TenantCreatedV1, TenantCreated] =
    event => toPersistentTenant(event.tenant).map(TenantCreated)

  implicit def tenantCreatedV1PersistEventSerializer: PersistEventSerializer[TenantCreated, TenantCreatedV1] =
    event => toProtobufTenant(event.tenant).map(ag => TenantCreatedV1.of(ag))

}
