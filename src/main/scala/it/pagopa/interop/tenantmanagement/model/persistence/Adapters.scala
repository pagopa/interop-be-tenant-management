package it.pagopa.interop.tenantmanagement.model.persistence

import cats.implicits._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.tenantmanagement.error.InternalErrors
import java.util.UUID
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantKind._

object Adapters {

  implicit class PersistentAttributesWrapper(private val p: PersistentTenantAttribute) extends AnyVal {
    def toAPI: TenantAttribute = p match {
      case a: PersistentCertifiedAttribute =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.CERTIFIED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = a.revocationTimestamp,
          extensionTimestamp = None,
          expirationTimestamp = None
        )
      case a: PersistentDeclaredAttribute  =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.DECLARED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = a.revocationTimestamp,
          extensionTimestamp = None,
          expirationTimestamp = None
        )
      case a: PersistentVerifiedAttribute  =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.VERIFIED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = a.revocationTimestamp,
          extensionTimestamp = a.extensionTimestamp,
          expirationTimestamp = a.expirationTimestamp.some
        )
    }
  }

  implicit class PersistentAttributesObjectWrapper(private val p: PersistentTenantAttribute.type) extends AnyVal {
    def fromAPI(attributes: TenantAttribute): Either[Throwable, PersistentTenantAttribute] = attributes.kind match {
      case TenantAttributeKind.CERTIFIED =>
        PersistentCertifiedAttribute(attributes.id, attributes.assignmentTimestamp, attributes.revocationTimestamp)
          .asRight[Throwable]
      case TenantAttributeKind.DECLARED  =>
        PersistentDeclaredAttribute(attributes.id, attributes.assignmentTimestamp, attributes.revocationTimestamp)
          .asRight[Throwable]
      case TenantAttributeKind.VERIFIED  =>
        attributes.expirationTimestamp
          .toRight(InternalErrors.InvalidAttribute("verified", "expirationTimestamp"))
          .map(expirationTimestamp =>
            PersistentVerifiedAttribute(
              id = attributes.id,
              assignmentTimestamp = attributes.assignmentTimestamp,
              revocationTimestamp = attributes.revocationTimestamp,
              extensionTimestamp = attributes.extensionTimestamp,
              expirationTimestamp = expirationTimestamp
            )
          )
    }
  }

  implicit class PersistentTenantExternalIdWrapper(private val p: PersistentTenantExternalId) extends AnyVal {
    def toAPI: ExternalId = ExternalId(origin = p.origin, value = p.value)
  }

  implicit class PersistentTenantExternalIdObjectWrapper(private val p: PersistentTenantExternalId.type)
      extends AnyVal {
    def fromAPI(e: ExternalId): PersistentTenantExternalId =
      PersistentTenantExternalId(origin = e.origin, value = e.value)
  }

  implicit class PersistentTenantWrapper(private val p: PersistentTenant) extends AnyVal {
    def toAPI: Tenant = Tenant(
      id = p.id,
      selfcareId = p.selfcareId.toString,
      kinds = p.kinds.map(_.toAPI),
      attributes = p.attributes.map(_.toAPI),
      externalId = p.externalId.toAPI
    )
  }

  implicit class PersistentTenantObjectWrapper(private val p: PersistentTenant.type) extends AnyVal {
    // We'll remove the field "id" from api and use just UUID.randomUUID once
    // migrated the persistence of all the already created eservices
    def fromAPI(seed: TenantSeed): Either[Throwable, PersistentTenant] =
      seed.attributes.toList
        .traverse(PersistentTenantAttribute.fromAPI)
        .map(attributes =>
          PersistentTenant(
            id = seed.id.getOrElse(UUID.randomUUID()),
            selfcareId = seed.selfcareId,
            externalId = PersistentTenantExternalId.fromAPI(seed.externalId),
            kinds = seed.kinds.map(PersistentTenantKind.fromAPI).toList,
            attributes = attributes
          )
        )
  }

  implicit class PersistentTenantKindWrapper(private val p: PersistentTenantKind) extends AnyVal {
    def toAPI: TenantKind = p match {
      case CERTIFIER => TenantKind.CERTIFIER
      case STANDARD  => TenantKind.STANDARD
    }
  }

  implicit class PersistentTenantKindObjectWrapper(private val p: PersistentTenantKind.type) extends AnyVal {
    def fromAPI(p: TenantKind): PersistentTenantKind = p match {
      case TenantKind.CERTIFIER => CERTIFIER
      case TenantKind.STANDARD  => STANDARD
    }
  }

}
