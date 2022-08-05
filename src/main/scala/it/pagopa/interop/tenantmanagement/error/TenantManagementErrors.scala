package it.pagopa.interop.tenantmanagement.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object TenantManagementErrors {
  case object CreateTenantConflict   extends ComponentError("0001", "Tenant already exists")
  case object CreateTenantBadRequest extends ComponentError("0002", "Error creating tenant - Bad Request")
  case class CreateTenantInvalidAttribute(attribute: String, field: String)
      extends ComponentError("0003", s"Error creating tenant - Invalid field $field in $attribute attribute")

  case object GetTenantNotFound   extends ComponentError("0004", "Tenant not found")
  case object GetTenantBadRequest extends ComponentError("0005", "Error getting tenant - Bad Request")
}
