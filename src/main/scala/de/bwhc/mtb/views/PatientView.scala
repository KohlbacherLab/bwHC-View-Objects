package de.bwhc.mtb.views



import java.time.{LocalDate,YearMonth}

import play.api.libs.json.Json

import de.bwhc.mtb.dtos.{
  Patient,
  Gender,
  HealthInsurance,
  Consent,
  ZPM
}


final case class PatientView
(
  id: Patient.Id,
  gender: String,
  birthDate: NotAvailable Or YearMonth,
  managingZPM: NotAvailable Or ZPM,
  insurance: NotAvailable Or HealthInsurance.Id,
  dateOfDeath: NotAvailable Or YearMonth,
  consentStatus: Consent.Status.Value,
  firstReferralDate: LocalDate
)


object PatientView
{

  import de.bwhc.util.json._
  import de.bwhc.util.json.time._

  implicit val format = Json.writes[PatientView]
}


