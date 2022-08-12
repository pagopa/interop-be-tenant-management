package it.pagopa.interop.tenantmanagement.model.persistence

import cats.implicits._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.tenantmanagement.error.InternalErrors
import java.util.UUID
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationStrictness.STANDARD
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationStrictness.STRICT

object Adapters {

  implicit class PersistentVerificationStrictnessWrapper(private val p: PersistentVerificationStrictness)
      extends AnyVal {
    def toAPI(): VerificationStrictness = p match {
      case STANDARD => VerificationStrictness.STANDARD
      case STRICT   => VerificationStrictness.STRICT
    }
  }

  implicit class PersistentVerificationStrictnessObjectWrapper(private val p: PersistentVerificationStrictness.type)
      extends AnyVal {
    def fromAPI(p: VerificationStrictness): PersistentVerificationStrictness = p match {
      case VerificationStrictness.STANDARD => STANDARD
      case VerificationStrictness.STRICT   => STRICT
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
      extentionDate = p.expirationDate,
      revocationDate = p.revocationDate
    )
  }

  implicit class PersistentVerificationTenantVerifierObjectWrapper(private val p: PersistentTenantVerifier.type)
      extends AnyVal {
    def fromAPI(p: TenantVerifier): PersistentTenantVerifier = PersistentTenantVerifier(
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
          strictness = None,
          verifiedBy = None,
          revokedBy = None
        )
      case a: PersistentDeclaredAttribute  =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.DECLARED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = a.revocationTimestamp,
          strictness = None,
          verifiedBy = None,
          revokedBy = None
        )
      case a: PersistentVerifiedAttribute  =>
        TenantAttribute(
          id = a.id,
          kind = TenantAttributeKind.VERIFIED,
          assignmentTimestamp = a.assignmentTimestamp,
          revocationTimestamp = None,
          strictness = a.strictness.toAPI().some,
          verifiedBy = None,
          revokedBy = None
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
        for {
          strictness <- attributes.strictness
            .toRight(InternalErrors.InvalidAttribute("verified", "strictness"))
            .map(PersistentVerificationStrictness.fromAPI)
          verifiedBy <- attributes.verifiedBy
            .toRight(InternalErrors.InvalidAttribute("verified", "verifiedBy"))
            .map(_.map(PersistentTenantVerifier.fromAPI).toList)
          // ! TODO it doesn't validate the fact that the revoked must always have a revocationDate
          revokedBy  <- attributes.revokedBy
            .toRight(InternalErrors.InvalidAttribute("verified", "revokedBy"))
            .map(_.map(PersistentTenantVerifier.fromAPI).toList)
        } yield PersistentVerifiedAttribute(
          id = attributes.id,
          assignmentTimestamp = attributes.assignmentTimestamp,
          strictness = strictness,
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
