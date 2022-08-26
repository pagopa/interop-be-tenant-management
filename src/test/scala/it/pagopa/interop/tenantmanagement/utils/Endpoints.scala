package it.pagopa.interop.tenantmanagement.utils

import akka.http.scaladsl.client.RequestBuilding.{Delete, Get, Post, Put}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpRequest
import it.pagopa.interop.commons.utils.USER_ROLES
import spray.json.DefaultJsonProtocol._
import it.pagopa.interop.commons.jwt._
import spray.json._

import java.util.UUID

case class Endpoints(endpoints: Set[Endpoint]) {
  def endpointsMap: Map[String, Endpoint] = endpoints.map(e => e.route -> e).toMap
}

case class Endpoint(route: String, verb: String, roles: Seq[String]) {

  def invalidRoles: Seq[Seq[(String, String)]] = AuthorizedRoutes.existingRoles
    .diff(roles)
    .map(role => Seq("bearer" -> "token", "uid" -> UUID.randomUUID().toString, USER_ROLES -> role))

  def rolesInContexts: Seq[Seq[(String, String)]] =
    roles.map(role => Seq("bearer" -> "token", "uid" -> UUID.randomUUID().toString, USER_ROLES -> role))

  def asRequest: HttpRequest = verb match {
    case "GET"    => Get()
    case "POST"   => Post()
    case "DELETE" => Delete()
    case "PUT"    => Put()
    // TODO make me safer, if you please
  }
}

object AuthorizedRoutes extends SprayJsonSupport {
  val existingRoles: List[String] = List(ADMIN_ROLE, SECURITY_ROLE, API_ROLE, M2M_ROLE, INTERNAL_ROLE)
  private val lines: String       = scala.io.Source.fromResource("authz.json").getLines().mkString
  implicit private val endpointFormat: RootJsonFormat[Endpoint]   = jsonFormat3(Endpoint)
  implicit private val endpointsFormat: RootJsonFormat[Endpoints] = jsonFormat1(Endpoints)
  val endpoints: Map[String, Endpoint]                            = lines.parseJson.convertTo[Endpoints].endpointsMap
}
