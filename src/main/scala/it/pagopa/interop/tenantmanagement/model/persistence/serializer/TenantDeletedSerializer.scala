package it.pagopa.interop.tenantmanagement.model.persistence.serializer

import akka.serialization.SerializerWithStringManifest
import it.pagopa.interop.tenantmanagement.model.persistence.TenantDeleted
import it.pagopa.interop.tenantmanagement.model.persistence.serializer.v1.tenantDeletedV1PersistEventSerializer

import java.io.NotSerializableException

class TenantDeletedSerializer extends SerializerWithStringManifest {

  final val version1: String = "1"

  final val currentVersion: String = version1

  override def identifier: Int = 100003

  override def manifest(o: AnyRef): String = s"${o.getClass.getName}|$currentVersion"

  final val className: String = classOf[TenantDeleted].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: TenantDeleted => serialize(event, className, currentVersion)
    case _                    =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[${manifest(o)}]], currentVersion: [[$currentVersion]] "
      )
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest.split('|').toList match {
    case `className` :: `version1` :: Nil =>
      deserialize(v1.events.TenantDeletedV1, bytes, manifest, currentVersion)
    case _                                =>
      throw new NotSerializableException(
        s"Unable to handle manifest: [[$manifest]], currentVersion: [[$currentVersion]] "
      )
  }

}
