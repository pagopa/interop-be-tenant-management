package it.pagopa.interop.tenantmanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.interop.tenantmanagement.model.persistence.TenantUpdated
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenantUpdatedV1PersistEventSerializer

import java.io.NotSerializableException

class TenantUpdatedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 100000

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val className: String = classOf[TenantUpdated].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: TenantUpdated => serialize(event, className, currentVersion)
    case _                    =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[${manifest(o)}]], currentVersion: [[$currentVersion]] "
      )
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case `className` :: `version1` :: Nil =>
      deserialize(v1.events.TenantUpdatedV1, bytes, manifest, currentVersion)
    case _                                =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )
  }

}