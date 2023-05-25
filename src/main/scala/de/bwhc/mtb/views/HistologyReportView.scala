package de.bwhc.mtb.views


import java.time.LocalDate

import play.api.libs.json.Json

import de.bwhc.mtb.dtos.{
  HistologyReport,
  TumorMorphology,
  TumorCellContent,
  Patient,
  Specimen,
  ICDO3M
}



case class ICDO3MDisplay(value: String) extends AnyVal

object ICDO3MDisplay
{
  implicit val format = Json.valueFormat[ICDO3MDisplay]
}


final case class HistologyReportView
(
  id: HistologyReport.Id,
  patient: Patient.Id,
  specimen: Specimen.Id,
  issuedOn: NotAvailable Or LocalDate,
  tumorMorphology: NotAvailable Or ICDO3MDisplay,
  tumorCellContent: NotAvailable Or TumorCellContentDisplay,
  note: String
)

object HistologyReportView
{
  import de.bwhc.util.json._

  implicit val format = Json.writes[HistologyReportView]
}
