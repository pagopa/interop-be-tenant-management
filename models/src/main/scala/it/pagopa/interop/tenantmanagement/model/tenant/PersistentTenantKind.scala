package it.pagopa.interop.tenantmanagement.model.tenant

sealed trait PersistentTenantKind

object PersistentTenantKind {
  val IPA: String = "IPA"
  val L37: String = "L37"
  val SAG: String = "SAG"

  case object Pa      extends PersistentTenantKind
  case object Gsp     extends PersistentTenantKind
  case object Private extends PersistentTenantKind

  def calculate(origin: String, value: String): PersistentTenantKind = (origin, value) match {
    case (PersistentTenantKind.IPA, PersistentTenantKind.L37) | (PersistentTenantKind.IPA, PersistentTenantKind.SAG) =>
      Gsp
    case (PersistentTenantKind.IPA, _) => Pa
    case (_, _)                        => Private
  }

}
