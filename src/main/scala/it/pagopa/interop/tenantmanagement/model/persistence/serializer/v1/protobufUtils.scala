package it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1

import cats.implicits._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenatmanagement.model.persistence.serializer.v1.tenant.TenantAttributeV1.Empty
import it.pagopa.interop.tenatmanagement.model.persistence.serializer.v1.tenant.{
  CertifiedAttributeV1,
  DeclaredAttributeV1,
  TenantAttributeV1,
  TenantV1,
  VerifiedAttributeV1
}

object protobufUtils {

  def toPersistentTenant(protobufTenant: TenantV1): Either[Throwable, PersistentTenant] = for {
    id         <- protobufTenant.id.toUUID.toEither
    selfcareId <- protobufTenant.selfcareId.toUUID.toEither
    attributes <- protobufTenant.attributes.traverse(toPersistentTenantAttributes)
  } yield PersistentTenant(
    id = id,
    selfcareId = selfcareId,
    externalId = null,
    kind = protobufTenant.kind,
    attributes = attributes.toList
  )

  def toProtobufTenant(persistentTenant: PersistentTenant): Either[Throwable, TenantV1] = TenantV1(
    id = persistentTenant.id.toString,
    selfcareId = persistentTenant.selfcareId.toString,
    externalId = null,
    kind = persistentTenant.kind,
    attributes = persistentTenant.attributes.map(toProtobufTenantAttribute)
  ).asRight[Throwable]

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
