package de.bwhc.mtb.views



import java.time.LocalDate

import play.api.libs.json.Json

import de.bwhc.mtb.dtos.{
  FamilyMemberDiagnosis,
  Patient
}


final case class FamilyMemberDiagnosisView
(
  id: FamilyMemberDiagnosis.Id,
  patient: Patient.Id,
  relationship: String
)


object FamilyMemberDiagnosisView
{
  implicit val format = Json.writes[FamilyMemberDiagnosisView]
}
