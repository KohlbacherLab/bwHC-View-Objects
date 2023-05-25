package de.bwhc.mtb.views


import java.time.temporal.Temporal

import play.api.libs.json.{Json,Format}

import de.bwhc.mtb.dtos.{
  Period,
  ClosedPeriod,
  OpenEndPeriod
}


final case class PeriodDisplay[T <: Temporal](value: String) extends AnyVal


object PeriodDisplay
{
  implicit def format[T <: Temporal: Format] = Json.valueWrites[PeriodDisplay[T]]

  implicit def dflt[T <: Temporal] = Default(PeriodDisplay[T]("N/A"))
}

