package it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1

import cats.syntax.all._
import it.pagopa.interop.commons.utils.TypeConversions.{LongOps, _}
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant.TenantAttributeV1.Empty
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantFeature.PersistentCertifier
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantKind.{GSP, PA, PRIVATE}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantMailKind.ContactEmail
import it.pagopa.interop.tenantmanagement.model.tenant._

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
    kind       <- protobufTenant.kind.traverse(toPersistentTenantKind)
    attributes <- protobufTenant.attributes.traverse(toPersistentTenantAttributes)
    features   <- protobufTenant.features.traverse(toPersistentTenantFeature)
    createdAt  <- protobufTenant.createdAt.toOffsetDateTime.toEither
    updatedAt  <- protobufTenant.updatedAt.traverse(_.toOffsetDateTime.toEither)
    mails      <- protobufTenant.mails.traverse(toPersistentTenantMail)
  } yield PersistentTenant(
    id = id,
    kind = kind,
    selfcareId = protobufTenant.selfcareId,
    externalId = externalId,
    features = features,
    attributes = attributes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mails = mails,
    name = protobufTenant.name.getOrElse("")
  )

  def toProtobufTenant(persistentTenant: PersistentTenant): Either[Throwable, TenantV1] = for {
    externalId <- toProtobufTenantExternalId(persistentTenant.externalId)
  } yield TenantV1(
    id = persistentTenant.id.toString,
    kind = persistentTenant.kind.map(toProtobufTenantKind),
    selfcareId = persistentTenant.selfcareId,
    externalId = externalId,
    features = persistentTenant.features.map(toProtobufTenantFeature),
    attributes = persistentTenant.attributes.map(toProtobufTenantAttribute),
    createdAt = persistentTenant.createdAt.toMillis,
    updatedAt = persistentTenant.updatedAt.map(_.toMillis),
    mails = persistentTenant.mails.map(toProtobufTenantMail),
    name = persistentTenant.name.some
  )

  def toPersistentTenantMail(protobufTenantMail: TenantMailV1): Either[Throwable, PersistentTenantMail] = for {
    kind      <- toPersistentTenantMailKind(protobufTenantMail.kind)
    createdAt <- protobufTenantMail.createdAt.toOffsetDateTime.toEither
  } yield PersistentTenantMail(
    kind = kind,
    address = protobufTenantMail.address,
    createdAt = createdAt,
    description = protobufTenantMail.description
  )

  def toProtobufTenantMail(persistentTenantMail: PersistentTenantMail): TenantMailV1 = TenantMailV1(
    kind = toProtobufTenantMailKind(persistentTenantMail.kind),
    address = persistentTenantMail.address,
    createdAt = persistentTenantMail.createdAt.toMillis,
    description = persistentTenantMail.description
  )

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

  def toProtobufTenantVerifier(verifier: PersistentTenantVerifier): TenantVerifierV1 = TenantVerifierV1(
    id = verifier.id.toString,
    verificationDate = verifier.verificationDate.toMillis,
    expirationDate = verifier.expirationDate.map(_.toMillis),
    extensionDate = verifier.extensionDate.map(_.toMillis)
  )

  def toPersistentTenantVerifier(verifier: TenantVerifierV1): Either[Throwable, PersistentTenantVerifier] = for {
    id               <- verifier.id.toUUID.toEither
    verificationDate <- verifier.verificationDate.toOffsetDateTime.toEither
    expirationDate   <- verifier.expirationDate.traverse(_.toOffsetDateTime.toEither)
    extensionDate    <- verifier.extensionDate.traverse(_.toOffsetDateTime.toEither)
  } yield PersistentTenantVerifier(
    id = id,
    verificationDate = verificationDate,
    expirationDate = expirationDate,
    extensionDate = extensionDate
  )

  def toProtobufTenantRevoker(revoker: PersistentTenantRevoker): TenantRevokerV1 = TenantRevokerV1(
    id = revoker.id.toString,
    verificationDate = revoker.verificationDate.toMillis,
    expirationDate = revoker.expirationDate.map(_.toMillis),
    extensionDate = revoker.extensionDate.map(_.toMillis),
    revocationDate = revoker.revocationDate.toMillis
  )

  def toPersistentTenantRevoker(verifier: TenantRevokerV1): Either[Throwable, PersistentTenantRevoker] = for {
    id               <- verifier.id.toUUID.toEither
    verificationDate <- verifier.verificationDate.toOffsetDateTime.toEither
    expirationDate   <- verifier.expirationDate.traverse(_.toOffsetDateTime.toEither)
    extensionDate    <- verifier.extensionDate.traverse(_.toOffsetDateTime.toEither)
    revocationDate   <- verifier.revocationDate.toOffsetDateTime.toEither
  } yield PersistentTenantRevoker(
    id = id,
    verificationDate = verificationDate,
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

  def toPersistentTenantKind(protobufTenantKind: TenantKindV1): Either[Throwable, PersistentTenantKind] =
    protobufTenantKind match {
      case TenantKindV1.PA                              => PA.asRight[Throwable]
      case TenantKindV1.GSP                             => GSP.asRight[Throwable]
      case TenantKindV1.PRIVATE                         => PRIVATE.asRight[Throwable]
      case TenantKindV1.Unrecognized(unrecognizedValue) =>
        new Exception(s"Unable to deserialize TenantKindV1 $unrecognizedValue").asLeft[PersistentTenantKind]
    }

  def toProtobufTenantKind(persistentTenantKind: PersistentTenantKind): TenantKindV1 =
    persistentTenantKind match {
      case PA      => TenantKindV1.PA
      case GSP     => TenantKindV1.GSP
      case PRIVATE => TenantKindV1.PRIVATE
    }
}
