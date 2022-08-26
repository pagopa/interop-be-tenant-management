package it.pagopa.interop.tenantmanagement.model.persistence

import cats.implicits._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.tenantmanagement.error.InternalErrors
import java.util.UUID
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationRenewal.AUTOMATIC_RENEWAL
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationRenewal.REVOKE_ON_EXPIRATION
import java.time.OffsetDateTime

object Adapters {

  implicit class PersistentVerificationRenewalWrapper(private val p: PersistentVerificationRenewal) extends AnyVal {
    def toAPI(): VerificationRenewal = p match {
      case AUTOMATIC_RENEWAL    => VerificationRenewal.AUTOMATIC_RENEWAL
      case REVOKE_ON_EXPIRATION => VerificationRenewal.REVOKE_ON_EXPIRATION
    }
  }

  implicit class PersistentVerificationRenewalObjectWrapper(private val p: PersistentVerificationRenewal.type)
      extends AnyVal {
    def fromAPI(p: VerificationRenewal): PersistentVerificationRenewal = p match {
      case VerificationRenewal.AUTOMATIC_RENEWAL    => AUTOMATIC_RENEWAL
      case VerificationRenewal.REVOKE_ON_EXPIRATION => REVOKE_ON_EXPIRATION
    }
  }

  implicit class PersistentTenantDeltaObjectWrapper(private val p: PersistentTenantDelta.type) extends AnyVal {
    def fromAPI(tenantId: String, td: TenantDelta): Either[Throwable, PersistentTenantDelta] = for {
      features <- td.features.toList.traverse(PersistentTenantFeature.fromAPI)
    } yield PersistentTenantDelta(id = tenantId, selfcareId = td.selfcareId, features = features)
  }

  implicit class PersistentVerificationTenantVerifierWrapper(private val p: PersistentTenantVerifier) extends AnyVal {
    def toAPI(): TenantVerifier = TenantVerifier(
      id = p.id,
      verificationDate = p.verificationDate,
      expirationDate = p.expirationDate,
      extentionDate = p.expirationDate
    )
  }

  implicit class PersistentVerificationTenantVerifierObjectWrapper(private val p: PersistentTenantVerifier.type)
      extends AnyVal {
    def fromAPI(p: TenantVerifier): PersistentTenantVerifier = PersistentTenantVerifier(
      id = p.id,
      verificationDate = p.verificationDate,
      expirationDate = p.expirationDate,
      extensionDate = p.expirationDate
    )
  }

  implicit class PersistentVerificationTenantRevokerWrapper(private val p: PersistentTenantRevoker) extends AnyVal {
    def toAPI(): PersistentTenantRevoker = PersistentTenantRevoker(
      id = p.id,
      verificationDate = p.verificationDate,
      expirationDate = p.expirationDate,
      extentionDate = p.expirationDate,
      revocationDate = p.revocationDate
    )
  }

  implicit class PersistentVerificationTenantRevokerObjectWrapper(private val p: PersistentTenantRevoker.type)
      extends AnyVal {
    def fromAPI(p: TenantRevoker): PersistentTenantRevoker = PersistentTenantRevoker(
      id = p.id,
      verificationDate = p.verificationDate,
      expirationDate = p.expirationDate,
      extentionDate = p.expirationDate,
      revocationDate = p.revocationDate
    )
  }

  implicit class PersistentAttributesWrapper(private val p: PersistentTenantAttribute) extends AnyVal {
    def toAPI: TenantAttribute = p match {
      case a: PersistentCertifiedAttribute =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.CERTIFIED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = a.revocationTimestamp,
          renewal = None,
          verifiedBy = None,
          revokedBy = None
        )
      case a: PersistentDeclaredAttribute  =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.DECLARED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = a.revocationTimestamp,
          renewal = None,
          verifiedBy = None,
          revokedBy = None
        )
      case a: PersistentVerifiedAttribute  =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.VERIFIED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = None,
          renewal = a.renewal.toAPI().some,
          verifiedBy = None,
          revokedBy = None
        )
    }
  }

  implicit class PersistentAttributesObjectWrapper(private val p: PersistentTenantAttribute.type) extends AnyVal {
    def fromAPI(attribute: TenantAttribute): Either[Throwable, PersistentTenantAttribute] =
      attribute.kind match {
        case TenantAttributeKind.CERTIFIED =>
          PersistentCertifiedAttribute(attribute.id, attribute.assignmentTimestamp, attribute.revocationTimestamp)
            .asRight[Throwable]
        case TenantAttributeKind.DECLARED  =>
          PersistentDeclaredAttribute(attribute.id, attribute.assignmentTimestamp, attribute.revocationTimestamp)
            .asRight[Throwable]
        case TenantAttributeKind.VERIFIED  =>
          for {
            renewal    <- attribute.renewal
              .toRight(InternalErrors.InvalidAttribute("verified", "renewal"))
              .map(PersistentVerificationRenewal.fromAPI)
            verifiedBy <- attribute.verifiedBy
              .toRight(InternalErrors.InvalidAttribute("verified", "verifiedBy"))
              .map(_.map(PersistentTenantVerifier.fromAPI).toList)
            revokedBy  <- attribute.revokedBy
              .toRight(InternalErrors.InvalidAttribute("verified", "revokedBy"))
              .map(_.map(PersistentTenantRevoker.fromAPI).toList)
          } yield PersistentVerifiedAttribute(
            id = attribute.id,
            assignmentTimestamp = attribute.assignmentTimestamp,
            renewal = renewal,
            verifiedBy = verifiedBy,
            revokedBy = revokedBy
          )
      }
  }

  implicit class PersistentTenantExternalIdWrapper(private val p: PersistentExternalId) extends AnyVal {
    def toAPI: ExternalId = ExternalId(origin = p.origin, value = p.value)
  }

  implicit class PersistentTenantExternalIdObjectWrapper(private val p: PersistentExternalId.type) extends AnyVal {
    def fromAPI(e: ExternalId): PersistentExternalId = PersistentExternalId(origin = e.origin, value = e.value)
  }

  implicit class PersistentTenantFeatureWrapper(private val p: PersistentTenantFeature) extends AnyVal {
    def toAPI: TenantFeature = p match {
      case PersistentTenantFeature.PersistentCertifier(certifierId) => TenantFeature(Certifier(certifierId).some)
    }
  }

  implicit class PersistentTenantFeatureObjectWrapper(private val p: PersistentTenantFeature.type) extends AnyVal {
    def fromAPI(e: TenantFeature): Either[Throwable, PersistentTenantFeature] = e match {
      case TenantFeature(Some(Certifier(certifierId))) =>
        PersistentTenantFeature.PersistentCertifier(certifierId).asRight[Throwable]
      case _                                           => InternalErrors.InvalidFeature.asLeft[PersistentTenantFeature]
    }
  }

  implicit class PersistentTenantWrapper(private val p: PersistentTenant) extends AnyVal {
    def toAPI: Tenant = Tenant(
      id = p.id,
      selfcareId = p.selfcareId,
      features = p.features.map(_.toAPI),
      attributes = p.attributes.map(_.toAPI),
      externalId = p.externalId.toAPI,
      createdAt = p.createdAt,
      updatedAt = p.updatedAt
    )

    def update(ptd: PersistentTenantDelta): PersistentTenant =
      p.copy(selfcareId = ptd.selfcareId, features = ptd.features)

    def getAttribute(id: UUID): Option[PersistentTenantAttribute] = p.attributes.find(_.id == id)
    def addAttribute(attr: PersistentTenantAttribute, time: OffsetDateTime): PersistentTenant = {
      val attribute: PersistentTenantAttribute = attr match {
        case x: PersistentCertifiedAttribute => x.copy(assignmentTimestamp = time)
        case x: PersistentDeclaredAttribute  => x.copy(assignmentTimestamp = time)
        case x: PersistentVerifiedAttribute  => x.copy(assignmentTimestamp = time)
      }
      p.copy(attributes = attribute :: p.attributes, updatedAt = time.some)
    }

    def updateAttribute(attr: PersistentTenantAttribute, time: OffsetDateTime): PersistentTenant =
      p.copy(attributes = attr :: p.attributes, updatedAt = time.some)
    def deleteAttribute(id: UUID, time: OffsetDateTime): PersistentTenant                        =
      p.copy(attributes = p.attributes.filterNot(_.id == id), updatedAt = time.some)
  }

  implicit class PersistentTenantObjectWrapper(private val p: PersistentTenant.type) extends AnyVal {
    // We'll remove the field "id" from api and use just UUID.randomUUID once
    // migrated the persistence of all the already created eservices
    def fromAPI(seed: TenantSeed, supplier: OffsetDateTimeSupplier): Either[Throwable, PersistentTenant] = for {
      attributes <- seed.attributes.toList.traverse(PersistentTenantAttribute.fromAPI)
      features   <- seed.features.toList.traverse(PersistentTenantFeature.fromAPI)
    } yield PersistentTenant(
      id = seed.id.getOrElse(UUID.randomUUID()),
      selfcareId = None,
      externalId = PersistentExternalId.fromAPI(seed.externalId),
      features = features,
      attributes = attributes,
      createdAt = supplier.get,
      updatedAt = None
    )

  }

}
