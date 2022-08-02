package it.pagopa.interop.tenantmanagement.common

import akka.util.Timeout

import scala.concurrent.duration.DurationInt

package object system {
  implicit val timeout: Timeout = 300.seconds
}
