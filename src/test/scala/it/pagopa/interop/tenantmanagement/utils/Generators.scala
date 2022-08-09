package it.pagopa.interop.tenantmanagement.utils

import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.state._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant._

import cats.implicits._
import org.scalacheck.Gen
import java.time.{OffsetDateTime, ZoneOffset}

object Generators {

  val stringGen: Gen[String] = for {
    n <- Gen.chooseNum(4, 100)
    s <- Gen.containerOfN[List, Char](n, Gen.alphaNumChar)
  } yield s.foldLeft("")(_ + _)

  val offsetDatetimeGen: Gen[(OffsetDateTime, Long)] = for {
    n <- Gen.chooseNum(0, 10000L)
    now      = OffsetDateTime.now(ZoneOffset.UTC)
    // Truncate to millis precision
    nowMills = now.withNano(now.getNano - (now.getNano % 1000000))
    time <- Gen.oneOf(nowMills.minusSeconds(n), nowMills.plusSeconds(n))
  } yield (time, time.toInstant.toEpochMilli)

  def listOf[T](g: => Gen[T]): Gen[List[T]] = for {
    n <- Gen.choose(0, 10)
    x <- Gen.listOfN(n, g)
  } yield x

  val externalIdGen: Gen[(PersistentTenantExternalId, ExternalIdV1)] = for {
    origin <- stringGen
    value  <- stringGen
  } yield (PersistentTenantExternalId(origin, value), ExternalIdV1(origin, value))

  val certifiedAttributeGen: Gen[(PersistentCertifiedAttribute, CertifiedAttributeV1)] = for {
    id                                    <- Gen.uuid
    (assignmentTimestamp, assignmentLong) <- offsetDatetimeGen
    (revocationTimestamp, revocationLong) <- Gen.option(offsetDatetimeGen).map(_.separate)
  } yield (
    PersistentCertifiedAttribute(id, assignmentTimestamp, revocationTimestamp),
    CertifiedAttributeV1(id.toString(), assignmentLong, revocationLong)
  )

  val declaredAttributeGen: Gen[(PersistentDeclaredAttribute, DeclaredAttributeV1)] = for {
    id                                    <- Gen.uuid
    (assignmentTimestamp, assignmentLong) <- offsetDatetimeGen
    (revocationTimestamp, revocationLong) <- Gen.option(offsetDatetimeGen).map(_.separate)
  } yield (
    PersistentDeclaredAttribute(id, assignmentTimestamp, revocationTimestamp),
    DeclaredAttributeV1(id.toString(), assignmentLong, revocationLong)
  )

  val verifiedAttributeGen: Gen[(PersistentVerifiedAttribute, VerifiedAttributeV1)] = for {
    id                                    <- Gen.uuid
    (assignmentTimestamp, assignmentLong) <- offsetDatetimeGen
    (revocationTimestamp, revocationLong) <- Gen.option(offsetDatetimeGen).map(_.separate)
    (expirationTimestamp, expirationLong) <- offsetDatetimeGen
    (extentionTimestamp, extentionLong)   <- Gen.option(offsetDatetimeGen).map(_.separate)
  } yield (
    PersistentVerifiedAttribute(
      id = id,
      assignmentTimestamp = assignmentTimestamp,
      revocationTimestamp = revocationTimestamp,
      extensionTimestamp = extentionTimestamp,
      expirationTimestamp = expirationTimestamp
    ),
    VerifiedAttributeV1(
      id = id.toString(),
      assignmentTimestamp = assignmentLong,
      revocationTimestamp = revocationLong,
      expirationTimestamp = expirationLong,
      extensionTimestamp = extentionLong
    )
  )

  val attributeGen: Gen[(PersistentTenantAttribute, TenantAttributeV1)] =
    Gen.oneOf(declaredAttributeGen, verifiedAttributeGen, certifiedAttributeGen)

  val tenantKindGen: Gen[(PersistentTenantKind, TenantKindV1)] = Gen.oneOf(
    (PersistentTenantKind.CERTIFIER, TenantKindV1.CERTIFIER),
    (PersistentTenantKind.STANDARD, TenantKindV1.STANDARD)
  )

  val tenantGen: Gen[(PersistentTenant, TenantV1)] = for {
    id                                               <- Gen.uuid
    selfcareId                                       <- stringGen
    (externalId, externalIdV1)                       <- externalIdGen
    (kinds, kindsV1)                                 <- listOf(tenantKindGen).map(_.separate)
    (persistentTenantAttributes, tenantAttributesV1) <- listOf(attributeGen).map(_.separate)
  } yield (
    PersistentTenant(id, selfcareId, externalId, kinds, persistentTenantAttributes),
    TenantV1(id.toString(), selfcareId.toString(), externalIdV1, kindsV1, tenantAttributesV1)
  )

  val stateGen: Gen[(State, StateV1)] = listOf(tenantGen).map(_.separate).map { case (ps, psv1) =>
    val state   = State(ps.map(p => p.id.toString -> p).toMap)
    val stateV1 = StateV1(psv1.map(pV1 => TenantsV1(pV1.id, pV1)))
    (state, stateV1)
  }

  implicit class PimpedStateV1(val stateV1: StateV1) extends AnyVal {
    def sorted: StateV1 = stateV1.copy(stateV1.tenants.sortBy(_.key))
  }

  val tenantCreatedGen: Gen[(TenantCreated, TenantCreatedV1)] = tenantGen.map { case (a, b) =>
    (TenantCreated(a), TenantCreatedV1(b))
  }

}
