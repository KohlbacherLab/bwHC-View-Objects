package de.bwhc.mtb.views



import java.time.LocalDate

import play.api.libs.json.Json

import de.bwhc.util.json._

import de.bwhc.mtb.dtos.{
  Patient,
  Claim,
  ClaimResponse,
  TherapyRecommendation
}


case class ClaimResponseStatusDisplay(value: String) extends AnyVal
object ClaimResponseStatusDisplay
{
  implicit val format = Json.valueFormat[ClaimResponseStatusDisplay]
}


case class ClaimResponseReasonDisplay(value: String) extends AnyVal
object ClaimResponseReasonDisplay
{
  implicit val format = Json.valueFormat[ClaimResponseReasonDisplay]
}


final case class ClaimStatusView
(
  id: Claim.Id,
  patient: Patient.Id,
  therapy: TherapyRecommendation.Id,
  issueDate: LocalDate,
  responseDate: NotAvailable Or LocalDate,
  status: NotAvailable Or ClaimResponseStatusDisplay,
  reason: NotAvailable Or ClaimResponseReasonDisplay 
)

object ClaimStatusView
{
  implicit val format = Json.writes[ClaimStatusView]
}
