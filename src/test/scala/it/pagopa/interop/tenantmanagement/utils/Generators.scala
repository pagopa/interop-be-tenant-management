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
    (strictness, strictnessV1)            <- verificationStrictnessGen
    (verifiedBy, verifiedByV1)            <- listOf(tenantVerifierGen).map(_.separate)
    (revokedBy, revokedByV1)              <- listOf(tenantVerifierGen).map(_.separate)
  } yield (
    PersistentVerifiedAttribute(
      id = id,
      assignmentTimestamp = assignmentTimestamp,
      strictness = strictness,
      verifiedBy = verifiedBy,
      revokedBy = revokedBy
    ),
    VerifiedAttributeV1(
      id = id.toString(),
      assignmentTimestamp = assignmentLong,
      strictness = strictnessV1,
      verifiedBy = verifiedByV1,
      revokedBy = revokedByV1
    )
  )

  val verificationStrictnessGen: Gen[(PersistentVerificationStrictness, VerificationStrictnessV1)] = Gen.oneOf(
    (PersistentVerificationStrictness.STANDARD, VerificationStrictnessV1.STANDARD),
    (PersistentVerificationStrictness.STRICT, VerificationStrictnessV1.STRICT)
  )

  val tenantVerifierGen: Gen[(PersistentTenantVerifier, TenantVerifierV1)] = for {
    id                                     <- Gen.uuid
    (verificationDate, verificationDateV1) <- offsetDatetimeGen
    (expirationDate, expirationDateV1)     <- Gen.option(offsetDatetimeGen).map(_.separate)
    (extentionDate, extentionDateV1)       <- Gen.option(offsetDatetimeGen).map(_.separate)
    (revocationDate, revocationDateV1)     <- Gen.option(offsetDatetimeGen).map(_.separate)
  } yield (
    PersistentTenantVerifier(id, verificationDate, expirationDate, extentionDate, revocationDate),
    TenantVerifierV1(id.toString(), verificationDateV1, expirationDateV1, extentionDateV1, revocationDateV1)
  )

  val attributeGen: Gen[(PersistentTenantAttribute, TenantAttributeV1)] =
    Gen.oneOf(declaredAttributeGen, verifiedAttributeGen, certifiedAttributeGen)

  val certifierGen: Gen[(PersistentTenantFeature.PersistentCertifier, CertifierV1)] =
    stringGen.map(certifierId => (PersistentTenantFeature.PersistentCertifier(certifierId), CertifierV1(certifierId)))

  val tenantFeature: Gen[(PersistentTenantFeature, TenantFeatureV1)] = for {
    (certifier, certifierV1) <- certifierGen
  } yield (certifier, certifierV1)

  val tenantGen: Gen[(PersistentTenant, TenantV1)] = for {
    id                                               <- Gen.uuid
    selfcareId                                       <- Gen.option(stringGen)
    (features, featuresV1)                           <- listOf(tenantFeature).map(_.separate)
    (externalId, externalIdV1)                       <- externalIdGen
    (persistentTenantAttributes, tenantAttributesV1) <- listOf(attributeGen).map(_.separate)
    (createdAt, createdAtV1)                         <- offsetDatetimeGen
    (updatedAt, updatedAtV1)                         <- Gen.option(offsetDatetimeGen).map(_.separate)
  } yield (
    PersistentTenant(id, selfcareId, externalId, features, persistentTenantAttributes, createdAt, updatedAt),
    TenantV1(id.toString(), selfcareId, externalIdV1, featuresV1, tenantAttributesV1, createdAtV1, updatedAtV1)
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
