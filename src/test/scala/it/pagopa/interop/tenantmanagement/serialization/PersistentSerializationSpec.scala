package it.pagopa.interop.tenantmanagement.serialization

import com.softwaremill.diffx.Diff
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.munit.DiffxAssertions
import it.pagopa.interop.tenantmanagement.model.persistence._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.events._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.state._
import it.pagopa.interop.tenantmanagement.model.persistence.serializer._
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import it.pagopa.interop.tenantmanagement.utils.Generators._

import scala.reflect.runtime.universe.{TypeTag, typeOf}

class PersistentSerializationSpec extends ScalaCheckSuite with DiffxAssertions {

  serdeCheck[State, StateV1](stateGen, _.sorted)
  deserCheck[State, StateV1](stateGen)
  serdeCheck[TenantCreated, TenantCreatedV1](tenantCreatedGen)
  deserCheck[TenantCreated, TenantCreatedV1](tenantCreatedGen)
  serdeCheck[TenantUpdated, TenantUpdatedV1](tenantUpdatedGen)
  deserCheck[TenantUpdated, TenantUpdatedV1](tenantUpdatedGen)
  serdeCheck[TenantDeleted, TenantDeletedV1](tenantDeletedGen)
  deserCheck[TenantDeleted, TenantDeletedV1](tenantDeletedGen)
  serdeCheck[SelfcareMappingCreated, SelfcareMappingCreatedV1](selfcareMappingCreatedGen)
  deserCheck[SelfcareMappingCreated, SelfcareMappingCreatedV1](selfcareMappingCreatedGen)

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
