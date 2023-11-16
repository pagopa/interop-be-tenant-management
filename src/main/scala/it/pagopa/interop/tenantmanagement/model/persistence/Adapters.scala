package it.pagopa.interop.tenantmanagement.model.persistence

import cats.implicits._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors.{InvalidAttributeStructure, InvalidFeature}
import it.pagopa.interop.tenantmanagement.model.MailKind.{CONTACT_EMAIL, DIGITAL_ADDRESS}
import it.pagopa.interop.tenantmanagement.model.TenantUnitType.{AOO, UO}
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantMailKind.{ContactEmail, DigitalAddress}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantUnitType.{Aoo, Uo}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantKind
import it.pagopa.interop.tenantmanagement.model.tenant._

import java.time.OffsetDateTime
import java.util.UUID

object Adapters {

  implicit class PersistentTenantDeltaObjectWrapper(private val p: PersistentTenantDelta.type) extends AnyVal {
    def fromAPI(tenant: PersistentTenant, td: TenantDelta): Either[Throwable, PersistentTenantDelta] = for {
      features <- td.features.toList.traverse(PersistentTenantFeature.fromAPI)
    } yield PersistentTenantDelta(
      id = tenant.id,
      selfcareId = td.selfcareId,
      features = features,
      kind = PersistentTenantKind.fromApi(td.kind).some
    )
  }

  implicit class PersistentVerificationTenantVerifierWrapper(private val p: PersistentTenantVerifier) extends AnyVal {
    def toAPI(): TenantVerifier = TenantVerifier(
      id = p.id,
      verificationDate = p.verificationDate,
      expirationDate = p.expirationDate,
      extensionDate = p.expirationDate
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
    def toAPI(): TenantRevoker = TenantRevoker(
      id = p.id,
      verificationDate = p.verificationDate,
      expirationDate = p.expirationDate,
      extensionDate = p.expirationDate,
      revocationDate = p.revocationDate
    )
  }

  implicit class PersistentVerificationTenantRevokerObjectWrapper(private val p: PersistentTenantRevoker.type)
      extends AnyVal {
    def fromAPI(p: TenantRevoker): PersistentTenantRevoker = PersistentTenantRevoker(
      id = p.id,
      verificationDate = p.verificationDate,
      expirationDate = p.expirationDate,
      extensionDate = p.expirationDate,
      revocationDate = p.revocationDate
    )
  }

  implicit class PersistentAttributesWrapper(private val p: PersistentTenantAttribute) extends AnyVal {
    def toAPI: TenantAttribute = p match {
      case a: PersistentCertifiedAttribute =>
        TenantAttribute(certified = CertifiedTenantAttribute(a.id, a.assignmentTimestamp, a.revocationTimestamp).some)
      case a: PersistentDeclaredAttribute  =>
        TenantAttribute(declared = DeclaredTenantAttribute(a.id, a.assignmentTimestamp, a.revocationTimestamp).some)
      case a: PersistentVerifiedAttribute  =>
        TenantAttribute(verified =
          VerifiedTenantAttribute(
            a.id,
            a.assignmentTimestamp,
            a.verifiedBy.map(_.toAPI()),
            a.revokedBy.map(_.toAPI())
          ).some
        )
    }
  }

  implicit class PersistentAttributesObjectWrapper(private val p: PersistentTenantAttribute.type) extends AnyVal {
    def fromAPI(attribute: TenantAttribute): Either[Throwable, PersistentTenantAttribute] = attribute match {
      case TenantAttribute(Some(declared), None, None)  =>
        PersistentDeclaredAttribute(declared.id, declared.assignmentTimestamp, declared.revocationTimestamp).asRight
      case TenantAttribute(None, Some(certified), None) =>
        PersistentCertifiedAttribute(certified.id, certified.assignmentTimestamp, certified.revocationTimestamp).asRight
      case TenantAttribute(None, None, Some(verified))  =>
        PersistentVerifiedAttribute(
          id = verified.id,
          assignmentTimestamp = verified.assignmentTimestamp,
          verifiedBy = verified.verifiedBy.toList.map(PersistentTenantVerifier.fromAPI),
          revokedBy = verified.revokedBy.toList.map(PersistentTenantRevoker.fromAPI)
        ).asRight
      case _                                            => InvalidAttributeStructure.asLeft
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
      case _                                           => InvalidFeature.asLeft[PersistentTenantFeature]
    }
  }

  implicit class PersistentTenantMailKindWrapper(private val ptmk: PersistentTenantMailKind) extends AnyVal {
    def toApi: MailKind = ptmk match {
      case ContactEmail   => MailKind.CONTACT_EMAIL
      case DigitalAddress => MailKind.DIGITAL_ADDRESS
    }
  }

  implicit class PersistentTenantMailKindObjectWrapper(private val p: PersistentTenantMailKind.type) extends AnyVal {
    def fromApi(mailKind: MailKind): PersistentTenantMailKind = mailKind match {
      case CONTACT_EMAIL   => ContactEmail
      case DIGITAL_ADDRESS => DigitalAddress
    }
  }

  implicit class PersistentTenantMailWrapper(private val ptm: PersistentTenantMail) extends AnyVal {
    def toApi: Mail = Mail(
      id = ptm.id,
      kind = ptm.kind.toApi,
      address = ptm.address,
      createdAt = ptm.createdAt,
      description = ptm.description.map(_.trim).filter(_.nonEmpty)
    )
  }

  implicit class PersistentTenantUnitTypeWrapper(private val ptut: PersistentTenantUnitType) extends AnyVal {
    def toApi: TenantUnitType = ptut match {
      case Aoo => TenantUnitType.AOO
      case Uo  => TenantUnitType.UO
    }
  }

  implicit class PersistentTenantUnitTypeObjectWrapper(private val p: PersistentTenantUnitType.type) extends AnyVal {
    def fromApi(subUnitType: TenantUnitType): PersistentTenantUnitType = subUnitType match {
      case AOO => Aoo
      case UO  => Uo
    }
  }

  implicit class PersistentTenantMailObjectWrapper(private val p: PersistentTenantMail.type) extends AnyVal {
    def fromAPI(ms: MailSeed, createdAt: OffsetDateTime): PersistentTenantMail = PersistentTenantMail(
      id = ms.id,
      kind = PersistentTenantMailKind.fromApi(ms.kind),
      address = ms.address.trim(),
      description = ms.description.map(_.trim).filterNot(_.isEmpty),
      createdAt = createdAt
    )
  }

  implicit class PersistentTenantWrapper(private val p: PersistentTenant) extends AnyVal {
    def toAPI: Tenant = Tenant(
      id = p.id,
      kind = p.kind.map(_.toApi),
      selfcareId = p.selfcareId,
      features = p.features.map(_.toAPI),
      attributes = p.attributes.map(_.toAPI),
      externalId = p.externalId.toAPI,
      createdAt = p.createdAt,
      updatedAt = p.updatedAt,
      mails = p.mails.map(_.toApi),
      name = p.name,
      onboardedAt = p.onboardedAt,
      subUnitType = p.subUnitType.map(_.toApi)
    )

    def update(ptd: PersistentTenantDelta, time: OffsetDateTime): PersistentTenant =
      p.copy(selfcareId = ptd.selfcareId, features = ptd.features, kind = ptd.kind, updatedAt = time.some)

    def getAttribute(id: UUID): Option[PersistentTenantAttribute] = p.attributes.find(_.id == id)

    def addAttribute(attr: PersistentTenantAttribute, time: OffsetDateTime): PersistentTenant =
      p.copy(attributes = attr :: p.attributes, updatedAt = time.some)

    def updateAttribute(attr: PersistentTenantAttribute, time: OffsetDateTime): PersistentTenant =
      p.copy(attributes = attr :: p.attributes.filterNot(_.id == attr.id), updatedAt = time.some)
  }

  implicit class PersistentTenantObjectWrapper(private val p: PersistentTenant.type) extends AnyVal {
    // We'll remove the field "id" from api and use just UUID.randomUUID once
    // migrated the persistence of all the already created eservices
    def fromAPI(seed: TenantSeed, supplier: OffsetDateTimeSupplier): Either[Throwable, PersistentTenant] = for {
      attributes <- seed.attributes.toList.traverse(PersistentTenantAttribute.fromAPI)
      features   <- seed.features.toList.traverse(PersistentTenantFeature.fromAPI)
    } yield PersistentTenant(
      id = seed.id.getOrElse(UUID.randomUUID()),
      kind = PersistentTenantKind.fromApi(seed.kind).some,
      selfcareId = None,
      externalId = PersistentExternalId.fromAPI(seed.externalId),
      features = features,
      attributes = attributes,
      createdAt = supplier.get(),
      updatedAt = None,
      mails = Nil,
      name = seed.name,
      onboardedAt = seed.onboardedAt,
      subUnitType = seed.subUnitType.map(PersistentTenantUnitType.fromApi)
    )
  }

  implicit class PersistentTenantKindWrapper(private val ptk: PersistentTenantKind) extends AnyVal {
    def toApi: TenantKind = ptk match {
      case PersistentTenantKind.PA      => TenantKind.PA
      case PersistentTenantKind.GSP     => TenantKind.GSP
      case PersistentTenantKind.PRIVATE => TenantKind.PRIVATE
    }
  }

  implicit class PersistentTenantKindObjectWrapper(private val ptk: PersistentTenantKind.type) extends AnyVal {
    def fromApi(tenantKind: TenantKind): PersistentTenantKind = tenantKind match {
      case TenantKind.PA      => PersistentTenantKind.PA
      case TenantKind.GSP     => PersistentTenantKind.GSP
      case TenantKind.PRIVATE => PersistentTenantKind.PRIVATE
    }
  }
}
