package it.pagopa.interop.tenantmanagement.model.persistence

import it.pagopa.interop.tenantmanagement.model.{Tenant, TenantAttributes, TenantSeed}
import it.pagopa.interop.tenantmanagement.model.tenant._

object Adapters {

  implicit class PersistentAttributesWrapper(private val p: PersistentTenantAttribute) extends AnyVal {
    def toAPI: TenantAttributes = ???
//      TenantAttributes(certified = p.certified, declared = p.declared, verified = p.verified)
  }

  implicit class PersistentAttributesObjectWrapper(private val p: PersistentTenantAttribute.type) extends AnyVal {
    def fromSeed(attributes: TenantAttributes): PersistentTenantAttribute = ???
//      PersistentTenantAttributes(
//        certified = attributes.certified.toList,
//        declared = attributes.declared.toList,
//        verified = attributes.verified.toList
//      )
  }

  implicit class PersistentTenantWrapper(private val p: PersistentTenant) extends AnyVal {
    def toAPI: Tenant = ???
//      Tenant(id = p.id, certifier = p.isCertifier, attributes = p.attributes.toAPI)

  }

  implicit class PersistentTenantObjectWrapper(private val p: PersistentTenant.type) extends AnyVal {
    def fromSeed(seed: TenantSeed): PersistentTenant = ???
//      PersistentTenant(
//        id = seed.id,
//        isCertifier = false,
//        attributes = PersistentTenantAttributes.fromSeed(seed.attributes)
//      )
  }

}
