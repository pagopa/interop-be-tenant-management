package it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1

import cats.implicits._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant.TenantAttributeV1.Empty
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant._
import it.pagopa.interop.commons.utils.TypeConversions.LongOps
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationStrictness._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantFeature.PersistentCertifier
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant.VerificationStrictnessV1.Unrecognized

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
    features   <- protobufTenant.features.toList.traverse(toPersistentTenantFeature)
    createdAt  <- protobufTenant.createdAt.toOffsetDateTime.toEither
    updatedAt  <- protobufTenant.updatedAt.traverse(_.toOffsetDateTime.toEither)
  } yield PersistentTenant(
    id = id,
    selfcareId = protobufTenant.selfcareId,
    externalId = externalId,
    features = features,
    attributes = attributes.toList,
    createdAt = createdAt,
    updatedAt = updatedAt
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
    updatedAt = persistentTenant.updatedAt.map(_.toMillis)
  )

  def toProtobufStrictness(strictness: PersistentVerificationStrictness): VerificationStrictnessV1 = strictness match {
    case STANDARD => VerificationStrictnessV1.STANDARD
    case STRICT   => VerificationStrictnessV1.STRICT
  }

  def toPersistentStrictness(
    strictness: VerificationStrictnessV1
  ): Either[Throwable, PersistentVerificationStrictness] =
    strictness match {
      case VerificationStrictnessV1.STANDARD => STANDARD.asRight[Throwable]
      case VerificationStrictnessV1.STRICT   => STRICT.asRight[Throwable]
      case Unrecognized(unrecognizedValue)   =>
        new Exception(s"Unable to deserialize VerificationStrictness $unrecognizedValue")
          .asLeft[PersistentVerificationStrictness]
    }

  def toProtobufTenantVerifier(verifier: PersistentTenantVerifier): TenantVerifierV1 = TenantVerifierV1(
    id = verifier.id.toString(),
    verificationDate = verifier.verificationDate.toMillis,
    expirationDate = verifier.expirationDate.map(_.toMillis),
    extentionDate = verifier.extentionDate.map(_.toMillis)
  )

  def toPersistentTenantVerifier(verifier: TenantVerifierV1): Either[Throwable, PersistentTenantVerifier] = for {
    id               <- verifier.id.toUUID.toEither
    verificationDate <- verifier.verificationDate.toOffsetDateTime.toEither
    expirationDate   <- verifier.expirationDate.traverse(_.toOffsetDateTime.toEither)
    extentionDate    <- verifier.extentionDate.traverse(_.toOffsetDateTime.toEither)
  } yield PersistentTenantVerifier(
    id = id,
    verificationDate = verificationDate,
    expirationDate = expirationDate,
    extentionDate = extentionDate
  )

  def toProtobufTenantRevoker(verifier: PersistentTenantRevoker): TenantRevokerV1 = TenantRevokerV1(
    id = verifier.id.toString(),
    verificationDate = verifier.verificationDate.toMillis,
    expirationDate = verifier.expirationDate.map(_.toMillis),
    extentionDate = verifier.extentionDate.map(_.toMillis),
    revocationDate = verifier.revocationDate.toMillis
  )

  def toPersistentTenantRevoker(verifier: TenantRevokerV1): Either[Throwable, PersistentTenantRevoker] = for {
    id               <- verifier.id.toUUID.toEither
    verificationDate <- verifier.verificationDate.toOffsetDateTime.toEither
    expirationDate   <- verifier.expirationDate.traverse(_.toOffsetDateTime.toEither)
    extentionDate    <- verifier.extentionDate.traverse(_.toOffsetDateTime.toEither)
    revocationDate   <- verifier.revocationDate.toOffsetDateTime.toEither
  } yield PersistentTenantRevoker(
    id = id,
    verificationDate = verificationDate,
    expirationDate = expirationDate,
    extentionDate = extentionDate,
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
    case VerifiedAttributeV1(id, assignmentTimestamp, strictn, vBy, rBy)    =>
      for {
        uuid       <- id.toUUID.toEither
        at         <- assignmentTimestamp.toOffsetDateTime.toEither
        verifiedBy <- vBy.traverse(toPersistentTenantVerifier)
        revokedBy  <- rBy.traverse(toPersistentTenantRevoker)
        strictness <- toPersistentStrictness(strictn)
      } yield PersistentVerifiedAttribute(
        id = uuid,
        assignmentTimestamp = at,
        strictness = strictness,
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
      case PersistentVerifiedAttribute(id, assignmentTimestamp, strictness, vBy, rBy) =>
        VerifiedAttributeV1(
          id = id.toString,
          assignmentTimestamp = assignmentTimestamp.toMillis,
          strictness = toProtobufStrictness(strictness),
          verifiedBy = vBy.map(toProtobufTenantVerifier),
          revokedBy = rBy.map(toProtobufTenantRevoker)
        )
    }

}
