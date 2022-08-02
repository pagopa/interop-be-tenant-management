package it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1

import it.pagopa.interop.commons.utils.TypeConversions.StringOps
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenatmanagement.model.persistence.serializer.v1.tenant.{TenantAttributesV1, TenantV1}

object protobufUtils {

  def toPersistentTenant(protobufTenant: TenantV1): Either[Throwable, PersistentTenant] =
    for {
      id         <- protobufTenant.id.toUUID.toEither
      attributes <- toPersistentTenantAttributes(protobufTenant.attributes)
    } yield PersistentTenant(id = id, certifier = protobufTenant.certifier, attributes = attributes)

  def toProtobufTenant(persistentTenant: PersistentTenant): Either[Throwable, TenantV1] =
    Right(
      TenantV1(
        id = persistentTenant.id.toString,
        certifier = persistentTenant.certifier,
        attributes = toProtobufTenantAttributes(persistentTenant.attributes)
      )
    )

  def toPersistentTenantAttributes(
    protobufTenantAttributes: TenantAttributesV1
  ): Either[Throwable, PersistentTenantAttributes] = Right(
    PersistentTenantAttributes(
      certified = protobufTenantAttributes.certified.toList,
      declared = protobufTenantAttributes.declared.toList,
      verified = protobufTenantAttributes.verified.toList
    )
  )

  def toProtobufTenantAttributes(persistentTenantAttributes: PersistentTenantAttributes): TenantAttributesV1 =
    TenantAttributesV1(
      certified = persistentTenantAttributes.certified,
      declared = persistentTenantAttributes.declared,
      verified = persistentTenantAttributes.verified
    )

}
