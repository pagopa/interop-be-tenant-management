package it.pagopa.interop.tenantmanagement.error

object InternalErrors {
  final case class NotFoundTenant(tenantId: String)       extends Throwable(s"Tenant $tenantId not found")
  final case class TenantAlreadyExists(tenantId: String)  extends Throwable(s"Tenant $tenantId already exists")
  final case class InvalidAttribute(attribute: String, field: String)
      extends Throwable(s"Invalid field $field in $attribute attribute")
  case object InvalidFeature                              extends Throwable(s"Invalid feature")
  case object MultipleTenantsForExternalId                extends Throwable(s"Internal server error")
  final case class NotFoundAttribute(attributeId: String) extends Throwable(s"Attribute $attributeId not found")
  final case class AttributeAlreadyExists(attributeId: String)
      extends Throwable(s"Attribute $attributeId already exists")
}
