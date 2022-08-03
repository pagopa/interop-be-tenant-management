package it.pagopa.interop.tenantmanagement.model.persistence.serializer

import cats.implicits._
import com.softwaremill.diffx.Diff
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.munit.DiffxAssertions
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.PersistentSerializationSpec._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.state._
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenatmanagement.model.persistence.serializer.v1.tenant.{TenantAttributesV1, TenantV1}
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID
import scala.reflect.runtime.universe.{TypeTag, typeOf}

class PersistentSerializationSpec extends ScalaCheckSuite with DiffxAssertions {

  serdeCheck[State, StateV1](stateGen, _.sorted)
  deserCheck[State, StateV1](stateGen)
  serdeCheck[TenantCreated, TenantCreatedV1](tenantCreatedGen)
  deserCheck[TenantCreated, TenantCreatedV1](tenantCreatedGen)

  // TODO move me in commons
  def serdeCheck[A: TypeTag, B](gen: Gen[(A, B)], adapter: B => B = identity[B](_))(implicit
    e: PersistEventSerializer[A, B],
    loc: munit.Location,
    d: => Diff[Either[Throwable, B]]
  ): Unit = property(s"${typeOf[A].typeSymbol.name.toString} is correctly serialized") {
    forAll(gen) { case (state, stateV1) =>
      implicit val diffX: Diff[Either[Throwable, B]] = d
      assertEqual(PersistEventSerializer.to[A, B](state).map(adapter), Right(stateV1).map(adapter))
    }
  }

  // TODO move me in commons
  def deserCheck[A, B: TypeTag](
    gen: Gen[(A, B)]
  )(implicit e: PersistEventDeserializer[B, A], loc: munit.Location, d: => Diff[Either[Throwable, A]]): Unit =
    property(s"${typeOf[B].typeSymbol.name.toString} is correctly deserialized") {
      forAll(gen) { case (state, stateV1) =>
        // * This is declared lazy in the signature to avoid a MethodTooBigException
        implicit val diffX: Diff[Either[Throwable, A]] = d
        assertEqual(PersistEventDeserializer.from[B, A](stateV1), Right(state))
      }
    }
}

object PersistentSerializationSpec {

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

  val persistentTenantAttributesGen: Gen[(PersistentTenantAttributes, TenantAttributesV1)] = for {
    certified <- listOf(UUID.randomUUID().toString)
    declared  <- listOf(UUID.randomUUID().toString)
    verified  <- listOf(UUID.randomUUID().toString)
  } yield (
    PersistentTenantAttributes(certified = certified, declared = declared, verified = verified),
    TenantAttributesV1(certified = certified, declared = declared, verified = verified)
  )

  val persistentTenantGen: Gen[(PersistentTenant, TenantV1)] = for {
    id                                               <- Gen.uuid
    certifier                                        <- Gen.oneOf(true, false)
    (persistentTenantAttributes, tenantAttributesV1) <- persistentTenantAttributesGen
  } yield (
    PersistentTenant(id = id, isCertifier = certifier, attributes = persistentTenantAttributes),
    TenantV1(id = id.toString, certifier = certifier, attributes = tenantAttributesV1)
  )

  val stateGen: Gen[(State, StateV1)] = listOf(persistentTenantGen).map(_.separate).map { case (ps, psv1) =>
    val state   = State(ps.map(p => p.id.toString -> p).toMap)
    val stateV1 = StateV1(psv1.map(pV1 => TenantsV1(pV1.id, pV1)))
    (state, stateV1)
  }

  implicit class PimpedStateV1(val stateV1: StateV1) extends AnyVal {
    def sorted: StateV1 = stateV1.copy(stateV1.tenants.sortBy(_.key))
  }

  val tenantCreatedGen: Gen[(TenantCreated, TenantCreatedV1)] = persistentTenantGen.map { case (a, b) =>
    (TenantCreated(a), TenantCreatedV1(b))
  }

}
