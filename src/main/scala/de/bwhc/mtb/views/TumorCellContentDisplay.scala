package de.bwhc.mtb.views


import play.api.libs.json.Json

import de.bwhc.mtb.dtos.TumorCellContent



case class TumorCellContentDisplay(value: String) extends AnyVal
object TumorCellContentDisplay
{
  implicit val format = Json.valueWrites[TumorCellContentDisplay]  
}

