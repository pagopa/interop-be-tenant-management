package it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1

import cats.implicits._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant.TenantAttributeV1.Empty
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant._

object protobufUtils {

  def toPersistentTenantExternalId(
    protobufTenantExternalId: ExternalIdV1
  ): Either[Throwable, PersistentTenantExternalId] =
    PersistentTenantExternalId(protobufTenantExternalId.origin, protobufTenantExternalId.value).asRight[Throwable]

  def toProtobufTenantExternalId(
    protobufTenantExternalId: PersistentTenantExternalId
  ): Either[Throwable, ExternalIdV1] =
    ExternalIdV1(protobufTenantExternalId.origin, protobufTenantExternalId.value).asRight[Throwable]

  def toPersistentTenant(protobufTenant: TenantV1): Either[Throwable, PersistentTenant] = for {
    id         <- protobufTenant.id.toUUID.toEither
    externalId <- toPersistentTenantExternalId(protobufTenant.externalId)
    attributes <- protobufTenant.attributes.traverse(toPersistentTenantAttributes)
  } yield PersistentTenant(
    id = id,
    selfcareId = protobufTenant.selfcareId,
    externalId = externalId,
    kind = protobufTenant.kind,
    attributes = attributes.toList
  )

  def toProtobufTenant(persistentTenant: PersistentTenant): Either[Throwable, TenantV1] =
    toProtobufTenantExternalId(persistentTenant.externalId).map(externalId =>
      TenantV1(
        id = persistentTenant.id.toString,
        selfcareId = persistentTenant.selfcareId.toString,
        externalId = externalId,
        kind = persistentTenant.kind,
        attributes = persistentTenant.attributes.map(toProtobufTenantAttribute)
      )
    )

  def toPersistentTenantAttributes(
    protobufTenantAttribute: TenantAttributeV1
  ): Either[Throwable, PersistentTenantAttribute] = protobufTenantAttribute match {
    case Empty                                                                 => Left(new RuntimeException("Booom"))
    case CertifiedAttributeV1(id, assignmentTimestamp, revocationTimestamp, _) =>
      for {
        uuid <- id.toUUID.toEither
        at   <- assignmentTimestamp.toOffsetDateTime.toEither
        rt   <- revocationTimestamp.traverse(_.toOffsetDateTime.toEither)
      } yield PersistentCertifiedAttribute(id = uuid, assignmentTimestamp = at, revocationTimestamp = rt)
    case DeclaredAttributeV1(id, assignmentTimestamp, revocationTimestamp, _)  =>
      for {
        uuid <- id.toUUID.toEither
        at   <- assignmentTimestamp.toOffsetDateTime.toEither
        rt   <- revocationTimestamp.traverse(_.toOffsetDateTime.toEither)
      } yield PersistentDeclaredAttribute(id = uuid, assignmentTimestamp = at, revocationTimestamp = rt)
    case VerifiedAttributeV1(
          id,
          assignmentTimestamp,
          revocationTimestamp,
          expirationTimestamp,
          extensionTimestamp,
          _
        ) =>
      for {
        uuid <- id.toUUID.toEither
        at   <- assignmentTimestamp.toOffsetDateTime.toEither
        rt   <- revocationTimestamp.traverse(_.toOffsetDateTime.toEither)
        ext  <- extensionTimestamp.traverse(_.toOffsetDateTime.toEither)
        exp  <- expirationTimestamp.toOffsetDateTime.toEither
      } yield PersistentVerifiedAttribute(
        id = uuid,
        assignmentTimestamp = at,
        revocationTimestamp = rt,
        extensionTimestamp = ext,
        expirationTimestamp = exp
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
      case PersistentVerifiedAttribute(
            id,
            assignmentTimestamp,
            revocationTimestamp,
            extensionTimestamp,
            expirationTimestamp
          ) =>
        VerifiedAttributeV1(
          id = id.toString,
          assignmentTimestamp = assignmentTimestamp.toMillis,
          revocationTimestamp = revocationTimestamp.map(_.toMillis),
          expirationTimestamp = expirationTimestamp.toMillis,
          extensionTimestamp = extensionTimestamp.map(_.toMillis)
        )
    }

}
