package it.pagopa.interop.tenantmanagement.provider

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import it.pagopa.interop.tenantmanagement._
import it.pagopa.interop.tenantmanagement.model.tenant._
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class TenantPersistentBehaviorSpec extends ScalaTestWithActorTestKit(SpecConfiguration.config) with AnyWordSpecLike {

  val tenantTemplate: PersistentTenant =
    PersistentTenant(
      id = UUID.randomUUID(),
      isCertifier = false,
      attributes = PersistentTenantAttributes(certified = List.empty, declared = List.empty, verified = List.empty)
    )

}
