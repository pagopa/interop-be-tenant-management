package it.pagopa.interop.tenantmanagement.utils

import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.state._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenant._

import cats.implicits._
import org.scalacheck.Gen
import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenantMailKind
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

  val externalIdGen: Gen[(PersistentExternalId, ExternalIdV1)] = for {
    origin <- stringGen
    value  <- stringGen
  } yield (PersistentExternalId(origin, value), ExternalIdV1(origin, value))

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

  val tenantVerifierGen: Gen[(PersistentTenantVerifier, TenantVerifierV1)] = for {
    id                                     <- Gen.uuid
    (verificationDate, verificationDateV1) <- offsetDatetimeGen
    (expirationDate, expirationDateV1)     <- Gen.option(offsetDatetimeGen).map(_.separate)
    (extensionDate, extensionDateV1)       <- Gen.option(offsetDatetimeGen).map(_.separate)
  } yield (
    PersistentTenantVerifier(id, verificationDate, expirationDate, extensionDate),
    TenantVerifierV1(id.toString(), verificationDateV1, expirationDateV1, extensionDateV1)
  )

  val tenantRevokerGen: Gen[(PersistentTenantRevoker, TenantRevokerV1)] = for {
    id                                     <- Gen.uuid
    (verificationDate, verificationDateV1) <- offsetDatetimeGen
    (expirationDate, expirationDateV1)     <- Gen.option(offsetDatetimeGen).map(_.separate)
    (extensionDate, extensionDateV1)       <- Gen.option(offsetDatetimeGen).map(_.separate)
    (revocationDate, revocationDateV1)     <- offsetDatetimeGen
  } yield (
    PersistentTenantRevoker(id, verificationDate, expirationDate, extensionDate, revocationDate),
    TenantRevokerV1(id.toString(), verificationDateV1, expirationDateV1, extensionDateV1, revocationDateV1)
  )

  val verifiedAttributeGen: Gen[(PersistentVerifiedAttribute, VerifiedAttributeV1)] = for {
    id                                    <- Gen.uuid
    (assignmentTimestamp, assignmentLong) <- offsetDatetimeGen
    (verifiedBy, verifiedByV1)            <- listOf(tenantVerifierGen).map(_.separate)
    (revokedBy, revokedByV1)              <- listOf(tenantRevokerGen).map(_.separate)
  } yield (
    PersistentVerifiedAttribute(
      id = id,
      assignmentTimestamp = assignmentTimestamp,
      verifiedBy = verifiedBy,
      revokedBy = revokedBy
    ),
    VerifiedAttributeV1(
      id = id.toString(),
      assignmentTimestamp = assignmentLong,
      verifiedBy = verifiedByV1,
      revokedBy = revokedByV1
    )
  )

  val attributeGen: Gen[(PersistentTenantAttribute, TenantAttributeV1)] =
    Gen.oneOf(declaredAttributeGen, verifiedAttributeGen, certifiedAttributeGen)

  val certifierGen: Gen[(PersistentTenantFeature.PersistentCertifier, CertifierV1)] =
    stringGen.map(certifierId => (PersistentTenantFeature.PersistentCertifier(certifierId), CertifierV1(certifierId)))

  val tenantFeature: Gen[(PersistentTenantFeature, TenantFeatureV1)] = for {
    (certifier, certifierV1) <- certifierGen
  } yield (certifier, certifierV1)

  val mailKindGenerator: Gen[(PersistentTenantMailKind, TenantMailKindV1)] =
    Gen.const((PersistentTenantMailKind.ContactEmail, TenantMailKindV1.CONTACT_EMAIL))

  val mailGenerator: Gen[(PersistentTenantMail, TenantMailV1)] = for {
    (persistentMailKind, protoMailKind) <- mailKindGenerator
    address                             <- stringGen
    (time, long)                        <- offsetDatetimeGen
    description                         <- Gen.option(stringGen)
  } yield (
    PersistentTenantMail(kind = persistentMailKind, address = address, description = description, createdAt = time),
    TenantMailV1(kind = protoMailKind, address = address, createdAt = long, description = description)
  )

  val kindGenerator: Gen[(PersistentTenantKind, TenantKindV1)] =
    Gen.oneOf(
      (PersistentTenantKind.PA, TenantKindV1.PA),
      (PersistentTenantKind.PRIVATE, TenantKindV1.PRIVATE),
      (PersistentTenantKind.GSP, TenantKindV1.GSP)
    )

  val tenantGen: Gen[(PersistentTenant, TenantV1)] = for {
    id                                               <- Gen.uuid
    selfcareId                                       <- Gen.option(stringGen)
    (features, featuresV1)                           <- listOf(tenantFeature).map(_.separate)
    (externalId, externalIdV1)                       <- externalIdGen
    (kind, kindV1)                                   <- kindGenerator
    (persistentTenantAttributes, tenantAttributesV1) <- listOf(attributeGen).map(_.separate)
    (createdAt, createdAtV1)                         <- offsetDatetimeGen
    (updatedAt, updatedAtV1)                         <- Gen.option(offsetDatetimeGen).map(_.separate)
    (mails, protoMails)                              <- listOf(mailGenerator).map(_.separate)
    name                                             <- stringGen
  } yield (
    PersistentTenant(
      id,
      kind.some,
      selfcareId,
      externalId,
      features,
      persistentTenantAttributes,
      createdAt,
      updatedAt,
      mails,
      name
    ),
    TenantV1(
      id.toString(),
      selfcareId,
      externalIdV1,
      featuresV1,
      tenantAttributesV1,
      createdAtV1,
      updatedAtV1,
      protoMails,
      name.some,
      kindV1.some
    )
  )

  val tenantsGen: Gen[(Map[String, PersistentTenant], List[TenantsV1])] = listOf(tenantGen).map(_.separate).map {
    case (pt, tv1) => (pt.map(t => (t.id.toString() -> t)).toMap, tv1.map(t1 => TenantsV1(t1.id, t1)))
  }

  val mappingsGen: Gen[(Map[String, UUID], List[TenantMappingV1])] = {
    val mappings: Gen[(String, UUID)] = for {
      selfcareId <- stringGen
      tenantId   <- Gen.uuid
    } yield (selfcareId, tenantId)

    for {
      entries <- listOf(mappings)
    } yield (entries.toMap, entries.map { case (k, v) => TenantMappingV1(k, v.toString()) })
  }

  val stateGen: Gen[(State, StateV1)] = for {
    (tenants, tenantsV1)                 <- tenantsGen
    (selcareMappings, selcareMappingsV1) <- mappingsGen
  } yield (State(tenants, selcareMappings), StateV1(tenantsV1, selcareMappingsV1))

  implicit class PimpedStateV1(val stateV1: StateV1) extends AnyVal {
    def sorted: StateV1 = stateV1.copy(stateV1.tenants.sortBy(_.key), stateV1.selcareMappings.sortBy(_.selfcareId))
  }

  val tenantCreatedGen: Gen[(TenantCreated, TenantCreatedV1)] = tenantGen.map { case (a, b) =>
    (TenantCreated(a), TenantCreatedV1(b))
  }

  val tenantUpdatedGen: Gen[(TenantUpdated, TenantUpdatedV1)] = tenantGen.map { case (a, b) =>
    (TenantUpdated(a), TenantUpdatedV1(b))
  }

  val tenantDeletedGen: Gen[(TenantDeleted, TenantDeletedV1)] = Gen.uuid.map(_.toString).map { uuid =>
    (TenantDeleted(uuid), TenantDeletedV1(uuid))
  }

  val selfcareMappingCreatedGen: Gen[(SelfcareMappingCreated, SelfcareMappingCreatedV1)] = for {
    selfcareId <- stringGen
    tenantId   <- Gen.uuid
  } yield (SelfcareMappingCreated(selfcareId, tenantId), SelfcareMappingCreatedV1(selfcareId, tenantId.toString()))

}
