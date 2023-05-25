package de.bwhc.mtb.views


import play.api.libs.json.Json

import de.bwhc.mtb.dtos.RECIST


case class ResponseDisplay(value: String) extends AnyVal
object ResponseDisplay
{
  implicit val format = Json.valueFormat[ResponseDisplay]
}
