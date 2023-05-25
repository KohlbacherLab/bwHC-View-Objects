package de.bwhc.mtb.views


import play.api.libs.json.Json

import de.bwhc.mtb.dtos._


final case class MTBFileView
(
  patient: PatientView,
  diagnoses: List[DiagnosisView],
  familyMemberDiagnoses: List[FamilyMemberDiagnosisView],
  guidelineTherapies: List[GuidelineTherapyView],
  ecogStatus: Option[ECOGStatusView],
  specimens: List[SpecimenView],
  molecularPathologyFindings: List[MolecularPathologyFindingView],
  histologyReports: List[HistologyReportView],
  ngsReports: List[NGSReportView],
  carePlans: List[CarePlanView],
  claimStatus: List[ClaimStatusView], 
  molecularTherapies: List[MolecularTherapyView]
)


object MTBFileView
{
  implicit val format = Json.writes[MTBFileView]
}
