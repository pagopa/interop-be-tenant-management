package it.pagopa.interop.tenantmanagement.model.persistence

import cats.implicits._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.error.TenantManagementErrors.{InvalidAttributeStructure, InvalidFeature}
import it.pagopa.interop.tenantmanagement.model.MailKind.CONTACT_EMAIL
import it.pagopa.interop.tenantmanagement.model._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantMailKind.ContactEmail
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantKind

import it.pagopa.interop.tenantmanagement.model.tenant._

import java.time.OffsetDateTime
import java.util.UUID

object Adapters {

  implicit class PersistentTenantDeltaObjectWrapper(private val p: PersistentTenantDelta.type) extends AnyVal {
    def fromAPI(
      tenant: PersistentTenant,
      td: TenantDelta,
      timeSupplier: OffsetDateTimeSupplier
    ): Either[Throwable, PersistentTenantDelta] = for {
      features <- td.features.toList.traverse(PersistentTenantFeature.fromAPI)
      actualMails    = tenant.mails.map(_.toApi)
      mailsToPersist = calculateMailsToKeep(timeSupplier)(actualMails, td.mails.toList)
    } yield PersistentTenantDelta(
      id = tenant.id,
      selfcareId = td.selfcareId,
      features = features,
      mails = mailsToPersist.map(PersistentTenantMail.fromApi),
      kind = PersistentTenantKind.fromApi(td.kind).some
    )

    private def calculateMailsToKeep(
      timeSupplier: OffsetDateTimeSupplier
    )(actualMails: List[Mail], tenantDeltaMails: List[MailSeed]): List[Mail] = tenantDeltaMails.map(ms =>
      actualMails
        .find(m => m.address == ms.address && m.kind == ms.kind)
        .getOrElse(ms.toModel(timeSupplier.get()))
        .copy(description = ms.description)
    )
  }

  implicit class MailSeedWrapper(private val ms: MailSeed) extends AnyVal {
    def toModel(createdAt: OffsetDateTime): Mail =
      Mail(kind = ms.kind, address = ms.address, createdAt = createdAt, description = ms.description)
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
      case ContactEmail => MailKind.CONTACT_EMAIL
    }
  }

  implicit class PersistentTenantMailKindObjectWrapper(private val p: PersistentTenantMailKind.type) extends AnyVal {
    def fromApi(mailKind: MailKind): PersistentTenantMailKind = mailKind match {
      case CONTACT_EMAIL => ContactEmail
    }
  }

  implicit class PersistentTenantMailWrapper(private val ptm: PersistentTenantMail) extends AnyVal {
    def toApi: Mail = Mail(
      kind = ptm.kind.toApi,
      address = ptm.address,
      createdAt = ptm.createdAt,
      description = ptm.description.map(_.trim).filter(_.nonEmpty)
    )
  }

  implicit class PersistentTenantMailObjectWrapper(private val p: PersistentTenantMail.type) extends AnyVal {
    def fromApi(mail: Mail): PersistentTenantMail = PersistentTenantMail(
      kind = PersistentTenantMailKind.fromApi(mail.kind),
      address = mail.address.trim(),
      createdAt = mail.createdAt,
      description = mail.description.map(_.trim).filterNot(_.isEmpty)
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
      name = p.name
    )

    def update(ptd: PersistentTenantDelta, time: OffsetDateTime): PersistentTenant =
      p.copy(
        selfcareId = ptd.selfcareId,
        features = ptd.features,
        mails = ptd.mails,
        kind = ptd.kind,
        updatedAt = time.some
      )

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
      name = seed.name
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
