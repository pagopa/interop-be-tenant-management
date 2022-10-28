package it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1

import cats.syntax.all._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant.TenantAttributeV1.Empty
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant._
import it.pagopa.interop.commons.utils.TypeConversions.LongOps
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationRenewal._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantFeature.PersistentCertifier
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant.PersistentVerificationRenewalV1.Unrecognized
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantMailKind.ContactEmail

object protobufUtils {

  def toPersistentTenantFeature(protobufTenantFeature: TenantFeatureV1): Either[Throwable, PersistentTenantFeature] =
    protobufTenantFeature match {
      case TenantFeatureV1.Empty    =>
        new Exception("Unable to deserialize PersistentTenantFeature").asLeft[PersistentTenantFeature]
      case CertifierV1(certifierId) => PersistentTenantFeature.PersistentCertifier(certifierId).asRight[Throwable]
    }

  def toProtobufTenantFeature(persistentTenantFeatures: PersistentTenantFeature): TenantFeatureV1 =
    persistentTenantFeatures match { case PersistentCertifier(certifierId) => CertifierV1(certifierId) }

  def toPersistentTenantExternalId(protobufTenantExternalId: ExternalIdV1): Either[Throwable, PersistentExternalId] =
    PersistentExternalId(protobufTenantExternalId.origin, protobufTenantExternalId.value).asRight[Throwable]

  def toProtobufTenantExternalId(protobufTenantExternalId: PersistentExternalId): Either[Throwable, ExternalIdV1] =
    ExternalIdV1(protobufTenantExternalId.origin, protobufTenantExternalId.value).asRight[Throwable]

  def toPersistentTenant(protobufTenant: TenantV1): Either[Throwable, PersistentTenant] = for {
    id         <- protobufTenant.id.toUUID.toEither
    externalId <- toPersistentTenantExternalId(protobufTenant.externalId)
    attributes <- protobufTenant.attributes.traverse(toPersistentTenantAttributes)
    features   <- protobufTenant.features.traverse(toPersistentTenantFeature)
    createdAt  <- protobufTenant.createdAt.toOffsetDateTime.toEither
    updatedAt  <- protobufTenant.updatedAt.traverse(_.toOffsetDateTime.toEither)
    mails      <- protobufTenant.mails.traverse(toPersistentTenantMail)
  } yield PersistentTenant(
    id = id,
    selfcareId = protobufTenant.selfcareId,
    externalId = externalId,
    features = features,
    attributes = attributes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mails = mails
  )

  def toProtobufTenant(persistentTenant: PersistentTenant): Either[Throwable, TenantV1] = for {
    externalId <- toProtobufTenantExternalId(persistentTenant.externalId)
  } yield TenantV1(
    id = persistentTenant.id.toString,
    selfcareId = persistentTenant.selfcareId,
    externalId = externalId,
    features = persistentTenant.features.map(toProtobufTenantFeature),
    attributes = persistentTenant.attributes.map(toProtobufTenantAttribute),
    createdAt = persistentTenant.createdAt.toMillis,
    updatedAt = persistentTenant.updatedAt.map(_.toMillis),
    mails = persistentTenant.mails.map(toProtobufTenantMail)
  )

  def toPersistentTenantMail(protobufTenantMail: TenantMailV1): Either[Throwable, PersistentTenantMail] = for {
    kind <- toPersistentTenantMailKind(protobufTenantMail.kind)
  } yield PersistentTenantMail(kind = kind, address = protobufTenantMail.address)

  def toProtobufTenantMail(persistentTenantMail: PersistentTenantMail): TenantMailV1 =
    TenantMailV1(kind = toProtobufTenantMailKind(persistentTenantMail.kind), address = persistentTenantMail.address)

  def toPersistentTenantMailKind(
    protobufTenantMailKind: TenantMailKindV1
  ): Either[Throwable, PersistentTenantMailKind] =
    protobufTenantMailKind match {
      case TenantMailKindV1.CONTACT_EMAIL                   => ContactEmail.asRight[Throwable]
      case TenantMailKindV1.Unrecognized(unrecognizedValue) =>
        new Exception(s"Unable to deserialize TenantMailKindV1 $unrecognizedValue").asLeft[PersistentTenantMailKind]
    }

  def toProtobufTenantMailKind(persistentTenantMailKind: PersistentTenantMailKind): TenantMailKindV1 =
    persistentTenantMailKind match {
      case ContactEmail => TenantMailKindV1.CONTACT_EMAIL
    }

  def toProtobufRenewal(renewal: PersistentVerificationRenewal): PersistentVerificationRenewalV1 = renewal match {
    case AUTOMATIC_RENEWAL    => PersistentVerificationRenewalV1.AUTOMATIC_RENEWAL
    case REVOKE_ON_EXPIRATION => PersistentVerificationRenewalV1.REVOKE_ON_EXPIRATION
  }

  def toPersistentRenewal(renewal: PersistentVerificationRenewalV1): Either[Throwable, PersistentVerificationRenewal] =
    renewal match {
      case PersistentVerificationRenewalV1.AUTOMATIC_RENEWAL    => AUTOMATIC_RENEWAL.asRight[Throwable]
      case PersistentVerificationRenewalV1.REVOKE_ON_EXPIRATION => REVOKE_ON_EXPIRATION.asRight[Throwable]
      case Unrecognized(unrecognizedValue)                      =>
        new Exception(s"Unable to deserialize VerificationRenewal $unrecognizedValue")
          .asLeft[PersistentVerificationRenewal]
    }

  def toProtobufTenantVerifier(verifier: PersistentTenantVerifier): TenantVerifierV1 = TenantVerifierV1(
    id = verifier.id.toString,
    verificationDate = verifier.verificationDate.toMillis,
    renewal = toProtobufRenewal(verifier.renewal),
    expirationDate = verifier.expirationDate.map(_.toMillis),
    extensionDate = verifier.extensionDate.map(_.toMillis)
  )

  def toPersistentTenantVerifier(verifier: TenantVerifierV1): Either[Throwable, PersistentTenantVerifier] = for {
    id               <- verifier.id.toUUID.toEither
    verificationDate <- verifier.verificationDate.toOffsetDateTime.toEither
    renewal          <- toPersistentRenewal(verifier.renewal)
    expirationDate   <- verifier.expirationDate.traverse(_.toOffsetDateTime.toEither)
    extensionDate    <- verifier.extensionDate.traverse(_.toOffsetDateTime.toEither)
  } yield PersistentTenantVerifier(
    id = id,
    verificationDate = verificationDate,
    renewal = renewal,
    expirationDate = expirationDate,
    extensionDate = extensionDate
  )

  def toProtobufTenantRevoker(revoker: PersistentTenantRevoker): TenantRevokerV1 = TenantRevokerV1(
    id = revoker.id.toString,
    verificationDate = revoker.verificationDate.toMillis,
    renewal = toProtobufRenewal(revoker.renewal),
    expirationDate = revoker.expirationDate.map(_.toMillis),
    extensionDate = revoker.extensionDate.map(_.toMillis),
    revocationDate = revoker.revocationDate.toMillis
  )

  def toPersistentTenantRevoker(verifier: TenantRevokerV1): Either[Throwable, PersistentTenantRevoker] = for {
    id               <- verifier.id.toUUID.toEither
    verificationDate <- verifier.verificationDate.toOffsetDateTime.toEither
    renewal          <- toPersistentRenewal(verifier.renewal)
    expirationDate   <- verifier.expirationDate.traverse(_.toOffsetDateTime.toEither)
    extensionDate    <- verifier.extensionDate.traverse(_.toOffsetDateTime.toEither)
    revocationDate   <- verifier.revocationDate.toOffsetDateTime.toEither
  } yield PersistentTenantRevoker(
    id = id,
    verificationDate = verificationDate,
    renewal = renewal,
    expirationDate = expirationDate,
    extensionDate = extensionDate,
    revocationDate = revocationDate
  )

  def toPersistentTenantAttributes(
    protobufTenantAttribute: TenantAttributeV1
  ): Either[Throwable, PersistentTenantAttribute] = protobufTenantAttribute match {
    case Empty                                                              => Left(new RuntimeException("Booom"))
    case CertifiedAttributeV1(id, assignmentTimestamp, revocationTimestamp) =>
      for {
        uuid <- id.toUUID.toEither
        at   <- assignmentTimestamp.toOffsetDateTime.toEither
        rt   <- revocationTimestamp.traverse(_.toOffsetDateTime.toEither)
      } yield PersistentCertifiedAttribute(id = uuid, assignmentTimestamp = at, revocationTimestamp = rt)
    case DeclaredAttributeV1(id, assignmentTimestamp, revocationTimestamp)  =>
      for {
        uuid <- id.toUUID.toEither
        at   <- assignmentTimestamp.toOffsetDateTime.toEither
        rt   <- revocationTimestamp.traverse(_.toOffsetDateTime.toEither)
      } yield PersistentDeclaredAttribute(id = uuid, assignmentTimestamp = at, revocationTimestamp = rt)
    case VerifiedAttributeV1(id, assignmentTimestamp, vBy, rBy)             =>
      for {
        uuid       <- id.toUUID.toEither
        at         <- assignmentTimestamp.toOffsetDateTime.toEither
        verifiedBy <- vBy.traverse(toPersistentTenantVerifier)
        revokedBy  <- rBy.traverse(toPersistentTenantRevoker)
      } yield PersistentVerifiedAttribute(
        id = uuid,
        assignmentTimestamp = at,
        verifiedBy = verifiedBy,
        revokedBy = revokedBy
      )
  }

  def toProtobufTenantAttribute(persistentTenantAttribute: PersistentTenantAttribute): TenantAttributeV1 =
    persistentTenantAttribute match {
      case PersistentCertifiedAttribute(id, assignmentTimestamp, revocationTimestamp) =>
        CertifiedAttributeV1(
          id = id.toString,
          assignmentTimestamp = assignmentTimestamp.toMillis,
          revocationTimestamp = revocationTimestamp.map(_.toMillis)
        )
      case PersistentDeclaredAttribute(id, assignmentTimestamp, revocationTimestamp)  =>
        DeclaredAttributeV1(
          id = id.toString,
          assignmentTimestamp = assignmentTimestamp.toMillis,
          revocationTimestamp = revocationTimestamp.map(_.toMillis)
        )
      case PersistentVerifiedAttribute(id, assignmentTimestamp, vBy, rBy)             =>
        VerifiedAttributeV1(
          id = id.toString,
          assignmentTimestamp = assignmentTimestamp.toMillis,
          verifiedBy = vBy.map(toProtobufTenantVerifier),
          revokedBy = rBy.map(toProtobufTenantRevoker)
        )
    }

}
