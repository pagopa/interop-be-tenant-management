package it.pagopa.interop.tenantmanagement.error

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID

object TenantManagementErrors {
  final case class TenantAlreadyExists(tenantId: String)
      extends ComponentError("0001", s"Tenant $tenantId already exists")

  final case class TenantNotFound(tenantId: String) extends ComponentError("0004", s"Tenant $tenantId not found")
  case object GetTenantBadRequest                   extends ComponentError("0005", "Error getting tenant - Bad Request")
  final case class AttributeAlreadyExists(attributeId: UUID)
      extends ComponentError("0006", s"Attribute $attributeId already exists")
  final case class AttributeNotFound(attributeId: String)
      extends ComponentError("0007", s"Attribute $attributeId not found")
  case object InvalidAttributeStructure             extends ComponentError("0008", "Invalid Attribute Structure")

  final case class TenantByExternalIdNotFound(origin: String, value: String)
      extends ComponentError("0008", s"Tenant with Origin $origin and Code $value not found")

  final case class MultipleTenantsForExternalId(origin: String, value: String, duplicatedIds: List[UUID])
      extends ComponentError(
        "0009",
        s"Multiple Tenants found for Origin $origin Code $value. Tenants: ${duplicatedIds.mkString("[", ",", "]")}"
      )

  final case class TenantBySelfcareIdNotFound(selfcareId: String)
      extends ComponentError("0010", s"Tenant with selfcareId $selfcareId not found")

  case object InvalidFeature extends ComponentError("0011", "Invalid feature for tenant")

}
