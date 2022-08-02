package it.pagopa.interop.tenantmanagement.error

import it.pagopa.interop.commons.utils.errors.ComponentError

object TenantManagementErrors {
  case object CreateTenantConflict   extends ComponentError("0001", "Tenant already existing")
  case object CreateTenantBadRequest extends ComponentError("0002", "Error while creating tenant - Bad Request")

  case object GetTenantNotFound   extends ComponentError("0003", "Tenant not found")
  case object GetTenantBadRequest extends ComponentError("0004", "Error while getting tenant - Bad Request")
}
