package de.bwhc.mtb.views


import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  Year
}
import java.time.temporal.Temporal
import java.time.format.DateTimeFormatter
import DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_INSTANT
}
import cats.Id
import cats.data.NonEmptyList
import cats.syntax.either._
import de.bwhc.util.num._
import de.bwhc.mtb.dtos._
import de.bwhc.catalogs.icd.{
  ICD10GMCatalogs,
  ICDO3Catalogs
}
import de.bwhc.catalogs.hgnc.{
  HGNCGene,
  HGNCCatalog,
  EnsemblId,
  HGNCId
}
import de.bwhc.catalogs.med.MedicationCatalog


trait mappings
{

  import ValueSets._
  import scala.util.chaining._
  import de.bwhc.mtb.dto.extensions.CodingExtensions._


  implicit val icd10gmCatalog: ICD10GMCatalogs

  implicit val icdO3Catalog: ICDO3Catalogs

  implicit val medicationCatalog: MedicationCatalog

  implicit val hgncCatalog: HGNCCatalog[Id]  



  implicit class MappingOps[T](val t: T)
  {
    def mapTo[V](implicit f: T => V) = f(t)
  }


  implicit class TemporalFormattingOps[T <: Temporal](val t: T)
  {
     private val ddMMyyyy = DateTimeFormatter.ofPattern("dd.MM.yyyy")

     def toISOFormat: String = {
       t match {
         case ld:  LocalDate     => ISO_LOCAL_DATE.format(ld)
         case ldt: LocalDateTime => ISO_LOCAL_DATE_TIME.format(ldt)
         case t:   Instant       => ISO_INSTANT.format(t)
       }
     }

     def toGermanDate: String = {
       ddMMyyyy.format(t)
     }

  }


  implicit val patientToView: ((Patient,Consent,MTBEpisode)) => PatientView = {
    case (pat,consent,episode) =>
      PatientView(
        pat.id,
        ValueSet[Gender.Value].displayOf(pat.gender).get,
        pat.birthDate.toRight(NotAvailable),
        pat.managingZPM.toRight(NotAvailable),
        pat.insurance.toRight(NotAvailable),
        pat.dateOfDeath.toRight(NotAvailable),
        consent.status,
        episode.period.start
      )
  }


  implicit def periodToDisplay[T <: Temporal, P <: Period[T]]: P => PeriodDisplay[T] = {

    case OpenEndPeriod(start,end) =>
      PeriodDisplay(s"${start.toISOFormat} - ${end.map(_.toISOFormat).getOrElse("N/A")}")

    case ClosedPeriod(start,end) =>
      PeriodDisplay(s"${start.toISOFormat} - ${end.toISOFormat}")

  }


/*
  implicit def icd10ToDisplay(
    implicit icd10gm: ICD10GMCatalogs
  ): Coding[ICD10GM] => ICD10Display = {
     icd10 =>     
       icd10gm.coding(
         icd.ICD10GM.Code(icd10.code.value),
       )
       .map(c => ICD10Display(s"${c.code.value}: ${c.display}"))
       .getOrElse(ICD10Display(s"${icd10.code.value}: ${icd10.display.getOrElse("N/A")}"))
  }


  implicit def icdO3TtoDisplay(
    implicit icdO3: ICDO3Catalogs
  ): Coding[ICDO3T] => ICDO3TDisplay = {
     icdO3T =>
      icdO3.topographyCodings()
        .find(_.code == icd.ICDO3.TopographyCode(icdO3T.code.value))
        .map(c => ICDO3TDisplay(s"${c.code.value}: ${c.display}"))
        .getOrElse(ICDO3TDisplay(s"${icdO3T.code.value}: ${icdO3T.display.getOrElse("N/A")}"))
  }


  implicit def icdO3MtoDisplay(
    implicit icdO3: ICDO3Catalogs
  ): Coding[ICDO3M] => ICDO3MDisplay = {
    icdO3M =>
      icdO3.morphologyCodings()
        .find(_.code == icd.ICDO3.MorphologyCode(icdO3M.code.value))
        .map(c => ICDO3MDisplay(s"${c.code.value}: ${c.display}"))
        .getOrElse(ICDO3MDisplay(s"${icdO3M.code.value}: ${icdO3M.display.getOrElse("N/A")}"))
  }
*/

  implicit val icd10ToDisplay: Coding[ICD10GM] => ICD10Display = 
    _.complete pipe (
      icd10 => ICD10Display(s"${icd10.code.value}: ${icd10.display.getOrElse("N/A")}")
    )

  implicit val icdO3TtoDisplay: Coding[ICDO3T] => ICDO3TDisplay = 
    _.complete pipe (
      icdo3 => ICDO3TDisplay(s"${icdo3.code.value}: ${icdo3.display.getOrElse("N/A")}")
    )
  
  implicit val icdO3MtoDisplay: Coding[ICDO3M] => ICDO3MDisplay = 
    _.complete pipe (
      icdo3 => ICDO3MDisplay(s"${icdo3.code.value}: ${icdo3.display.getOrElse("N/A")}")
    )
  
/*
  implicit def medicationCodingsToDisplay(
    implicit medications: MedicationCatalog
  ): List[Medication.Coding] => MedicationDisplay = {
    meds =>
      MedicationDisplay(
        meds.map(
          coding =>

            coding.system match {

              case Medication.System.ATC => 
                (
                 for {
                   version <- coding.version
                   med     <- medications.findWithCode(coding.code.value,version)
                   clss    <- med.parent.flatMap(medications.find(_,version))
                 } yield s"${med.name} (Klasse: ${clss.name})"
                )
                .getOrElse(s"${coding.display.getOrElse("N/A")} (${coding.code.value})")

              case Medication.System.Unregistered =>
                s"${coding.display.getOrElse("N/A")} (${coding.code.value})"

            }
        )
        .reduceLeftOption(_ + ", " + _)
        .getOrElse("N/A")
      )
  }


  implicit def medicationCodingsToDisplayWithClasses(
    implicit medications: MedicationCatalog
  ): List[Medication.Coding] => (MedicationDisplay,MedicationDisplay) = {
    meds =>

      val (drugs,classes) = 
        meds.foldLeft(
          (List.empty[String],List.empty[String])
        ){
          case ((drgs,clsses),coding) =>
        
            coding.system match {
        
              case Medication.System.ATC => {
                (
                 for {
                   version <- coding.version
                   med     <- medications.findWithCode(coding.code.value,version)
                   clss    =  med.parent.flatMap(medications.find(_,version))
                 } yield {
                   (drgs :+ med.name, clsses ++ clss.map(_.name))
                 }
                )
                .getOrElse(
                  (drgs :+ s"${coding.display.getOrElse("N/A")} (${coding.code.value})", clsses)
                )
              }
        
            case Medication.System.Unregistered =>
              (drgs :+ s"${coding.display.getOrElse("N/A")} (${coding.code.value})", clsses)
        
          }
        }

    (
     MedicationDisplay(Option(drugs).filter(_.nonEmpty).map(_.mkString(", ")).getOrElse("N/A")),
     MedicationDisplay(Option(classes).filter(_.nonEmpty).map(_.mkString(", ")).getOrElse("N/A")),
    )
      
  }
*/

  implicit val medicationCodingToDisplay: Medication.Coding => MedicationDisplay = {
    med =>
      MedicationDisplay(
        med.complete pipe (coding => s"${coding.display.getOrElse("N/A")} (${coding.code.value})")
      )
  }


  implicit val medicationCodingsToDisplay: List[Medication.Coding] => MedicationDisplay = {
    meds =>
      MedicationDisplay(
        meds.map(
          _.complete pipe (coding => s"${coding.display.getOrElse("N/A")} (${coding.code.value})")
        )
        .reduceLeftOption(_ + ", " + _)
        .getOrElse("N/A")
      )
  }


  implicit val medicationCodingsToDisplayWithClasses: List[Medication.Coding] => (MedicationDisplay,MedicationDisplay) = {
    meds =>

      val (drugs,classes) = 
        meds.foldLeft(
          (List.empty[Medication.Coding],List.empty[Medication.Coding])
        ){
          case ((drgs,clsses),coding) =>
            (
              drgs :+ coding.complete, 
              clsses ++ coding.medicationGroup
            )
        }  

      drugs.mapTo[MedicationDisplay] -> classes.mapTo[MedicationDisplay]
        
  }



  implicit val diagnosisToView: Diagnosis => DiagnosisView = {
    diag => 
      DiagnosisView(
        diag.id,
        diag.patient,
        diag.recordedOn.toRight(NotAvailable),
        diag.icd10.map(_.mapTo[ICD10Display]).toRight(NotAvailable),
        diag.icdO3T.map(_.mapTo[ICDO3TDisplay]).toRight(NotAvailable),
        diag.whoGrade
          .map(_.code)
          .flatMap(
            c =>
              ValueSet[WHOGrade.Value].displayOf(c)
                .map(d => WHOGradeDisplay(s"${c}: ${d}"))
          )
          .toRight(NotAvailable),
        diag.statusHistory
          .filterNot(_.isEmpty)
          .map(_.sortWith((t1,t2) => t1.date isBefore t2.date))
          .flatMap {
            _.map{
              case Diagnosis.StatusOnDate(status,date) =>
                s"${date.toISOFormat}: ${ValueSet[Diagnosis.Status.Value].displayOf(status).get}"
            }
            .reduceLeftOption(_ + ", " + _)
          } 
          .toRight(NotAvailable),
        diag.guidelineTreatmentStatus
          .flatMap(ValueSet[GuidelineTreatmentStatus.Value].displayOf)
          .toRight(NotAvailable)

      )
  }


  implicit val famMemDiagnosisToView: FamilyMemberDiagnosis => FamilyMemberDiagnosisView = {
    fmdiag =>
      FamilyMemberDiagnosisView(
        fmdiag.id,
        fmdiag.patient,
        ValueSet[FamilyMember.Relationship.Value]
          .displayOf(fmdiag.relationship.code)
          .get
      )
  }



  implicit val responseToDisplay: Response => ResponseDisplay = {
    resp =>
      ValueSet[RECIST.Value]
        .displayOf(resp.value.code)
        .map(ResponseDisplay(_))
        .get
  }


  implicit def guidelineTherapyToView[T <: GuidelineTherapy]:
    ((
     T,
     Option[Diagnosis],
     Option[Response])) => GuidelineTherapyView = {

    case (therapy,diagnosis,response) =>

      val (medication,medicationClasses) =
        therapy.medication
          .map(_.mapTo[(MedicationDisplay,MedicationDisplay)]).unzip

      therapy match { 
      
        case th: PreviousGuidelineTherapy =>
          GuidelineTherapyView(
            th.id,
            th.patient,
            diagnosis.flatMap(_.icd10.map(_.mapTo[ICD10Display])).toRight(NotAvailable),
            th.therapyLine.toRight(NotAvailable),
            NotAvailable.asLeft[PeriodDisplay[LocalDate]],
            medication.toRight(NotAvailable),
            medicationClasses.toRight(NotAvailable),
            NotAvailable.asLeft[String],
            response.map(_.mapTo[ResponseDisplay]).toRight(NotAvailable),
            response.filter(_.value.code == RECIST.PD).map(_.effectiveDate).toRight(Undefined),
          )
        
        case th: LastGuidelineTherapy =>
          GuidelineTherapyView(
            th.id,
            th.patient,
            diagnosis.flatMap(_.icd10.map(_.mapTo[ICD10Display])).toRight(NotAvailable),
            th.therapyLine.toRight(NotAvailable),
            th.period.map(_.mapTo[PeriodDisplay[LocalDate]]).toRight(NotAvailable),
            medication.toRight(NotAvailable),
            medicationClasses.toRight(NotAvailable),
            th.reasonStopped
              .flatMap(c => ValueSet[GuidelineTherapy.StopReason.Value].displayOf(c.code))
              .toRight(NotAvailable),
            response.map(_.mapTo[ResponseDisplay]).toRight(NotAvailable),
            response.filter(_.value.code == RECIST.PD).map(_.effectiveDate).toRight(Undefined),
          )
      
      }

  }

  implicit val ecogToDisplay: Coding[ECOG.Value] => ECOGDisplay = {
    coding =>
      ValueSet[ECOG.Value]
        .displayOf(coding.code)
        .map(ECOGDisplay(_))
        .get  // safe to call 
  }

/*
  import scala.util.matching.Regex

  private val ecog = "(ECOG\\s\\d)".r.unanchored
 
  implicit val ecogToDisplay: Coding[ECOG.Value] => ECOGDisplay = {
    coding =>
      ValueSet[ECOG.Value]
        .displayOf(coding.code)
        .map { case ecog(code) => code }
        .map(ECOGDisplay(_))
        .get  // safe to call 
  }
*/

  implicit val ecogsToDisplay: ((Patient,List[ECOGStatus])) => ECOGStatusView = {
    case (patient,ecogs) =>
      ECOGStatusView(
        patient.id,
        ecogs.map(ecog =>
          DatedValue(
            ecog.effectiveDate.map(_.toISOFormat).getOrElse("N/A"),
            ecog.value.mapTo[ECOGDisplay]
          )
        )
      )
  }



  implicit val specimenToView: Specimen => SpecimenView = {
    specimen =>
      SpecimenView(
        specimen.id,
        specimen.patient,
        specimen.icd10.mapTo[ICD10Display],
        specimen.`type`.flatMap(ValueSet[Specimen.Type.Value].displayOf)
          .toRight(NotAvailable),
        specimen.collection.map(_.date).toRight(NotAvailable),
        specimen.collection.map(_.localization)
          .flatMap(ValueSet[Specimen.Collection.Localization.Value].displayOf)
          .toRight(NotAvailable),
        specimen.collection.map(_.method)
          .flatMap(ValueSet[Specimen.Collection.Method.Value].displayOf)
          .toRight(NotAvailable),
      )
  }


  implicit val molPathoToView: MolecularPathologyFinding => MolecularPathologyFindingView = {
    finding =>
      MolecularPathologyFindingView(
        finding.id,
        finding.patient,
        finding.specimen,
        finding.performingInstitute.toRight(NotAvailable),
        finding.issuedOn.toRight(NotAvailable),
        finding.note
      )
  }



  implicit val tumorCellContentToDisplay: TumorCellContent => TumorCellContentDisplay = {
    tc =>
      val valuePercent = (tc.value * 100).toInt
      val method       = ValueSet[TumorCellContent.Method.Value].displayOf(tc.method).get

      TumorCellContentDisplay(s"$valuePercent % ($method)")
  }


  implicit val histologyReportToView: HistologyReport => HistologyReportView = {
    report =>
      HistologyReportView(
        report.id,
        report.patient,
        report.specimen,
        report.issuedOn.toRight(NotAvailable),
        report.tumorMorphology.map(_.value.mapTo[ICDO3MDisplay]).toRight(NotAvailable),
        report.tumorCellContent.map(_.mapTo[TumorCellContentDisplay]).toRight(NotAvailable),
        report.tumorMorphology.flatMap(_.note).getOrElse("-"),
      )
  }


  import SomaticNGSReport._
 

  implicit val tmbToDisplay: TMB => TMBDisplay = {
    tmb => TMBDisplay(s"${tmb.value} mut/MBase")   
  }


  import Variant.{StartEnd}
  import Gene.{HgncId}

  implicit val startEndToDisplay: StartEnd => StartEndDisplay = {
    case StartEnd(start,optEnd) =>
      optEnd.map(
        end => StartEndDisplay(s"${start} - $end")
      )
      .getOrElse(  
        StartEndDisplay(start.toString)
      )
  }

/*
  implicit def geneCodingToDisplay(
    implicit hgnc: HGNCCatalog[cats.Id]
  ): Gene.Coding => GeneDisplay = {
    coding =>
      coding.hgncId
        .flatMap(id => hgncCatalog.gene(HGNCId(id.value)))
        .orElse(
          coding.ensemblId
            .map(_.value)
            .flatMap(hgncCatalog.geneWithEnsemblId)
        )
        .map(g => GeneDisplay(g.symbol))
        .getOrElse(GeneDisplay("N/A"))

  }
*/

  implicit val geneCodingToDisplay: Gene.Coding => GeneDisplay = {
    gene =>
      GeneDisplay(
        gene.complete
          .symbol
          .map(_.value)
          .getOrElse("N/A")
      )
  }


  implicit def geneCodingsToDisplay: List[Gene.Coding] => Option[GeneDisplay] = {
    genes =>
      genes.map(_.complete.symbol.map(_.value).getOrElse("N/A"))
        .reduceLeftOption(_ + ", " + _)
        .map(GeneDisplay(_))
  }

  implicit val simpleVariantToView: SimpleVariant => SimpleVariantView = {
    sv =>
      SimpleVariantView(
        sv.patient,
        sv.chromosome,
        sv.gene.map(_.mapTo[GeneDisplay]).toRight(NotAvailable),
        sv.startEnd.mapTo[StartEndDisplay],
        sv.refAllele,
        sv.altAllele,
        sv.dnaChange.map(_.code).toRight(Undefined),
        sv.aminoAcidChange.map(_.code).toRight(Undefined),
        sv.readDepth,
        sv.allelicFrequency,
        sv.cosmicId.toRight(NotAvailable),
        sv.dbSNPId.toRight(NotAvailable),
        sv.interpretation.code
      )
  }


  implicit val cnvToView: CNV => CNVView = {
    cnv =>
      CNVView(
        cnv.patient,
        cnv.chromosome,
        cnv.reportedAffectedGenes
          .flatMap(_.mapTo[Option[GeneDisplay]])
          .toRight(NotAvailable),
        cnv.startRange.mapTo[StartEndDisplay],
        cnv.endRange.mapTo[StartEndDisplay],
        cnv.totalCopyNumber.toRight(NotAvailable),
        cnv.relativeCopyNumber.toRight(NotAvailable),
        cnv.cnA.toRight(NotAvailable),
        cnv.cnB.toRight(NotAvailable),
        cnv.reportedFocality.toRight(NotAvailable),
        cnv.`type`,
        cnv.copyNumberNeutralLoH
          .flatMap(_.mapTo[Option[GeneDisplay]])
          .toRight(NotAvailable)
      )
  }


  implicit val dnaFusionToView: DNAFusion => DNAFusionView = {
    case fus @ DNAFusion(
      _,
      _,
      partner5pr,
      partner3pr,
      numReads
    ) =>

      val symbol5pr = partner5pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")
      val symbol3pr = partner3pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")
      val chrPos5pr = partner5pr.map(p => s"${p.chromosome.value}:${p.position}").getOrElse("-")
      val chrPos3pr = partner3pr.map(p => s"${p.chromosome.value}:${p.position}").getOrElse("-")

      DNAFusionView(
        fus.patient,
        s"$symbol5pr :: $symbol3pr ($chrPos5pr :: $chrPos3pr)",
        numReads.toRight(NotAvailable)
      )

  }


  implicit val rnaFusionToView: RNAFusion => RNAFusionView = {
    case fus @ RNAFusion(
      _,
      _,
      partner5pr,
      partner3pr,
      effect,
      cosmicId,
      numReads
    ) =>

    val symbol5pr = partner5pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")
    val symbol3pr = partner3pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")

    val transcriptExon5pr = partner5pr.map(p => s"${p.transcriptId.value}: ${p.exon.value}").getOrElse("-")
    val transcriptExon3pr = partner3pr.map(p => s"${p.transcriptId.value}: ${p.exon.value}").getOrElse("-")

    RNAFusionView(
      fus.patient,
      s"$symbol5pr ($transcriptExon5pr) :: $symbol3pr ($transcriptExon3pr)",
      partner5pr.map(_.position).toRight(Intergenic),
      partner5pr.map(_.strand).toRight(Intergenic),
      partner3pr.map(_.position).toRight(Intergenic),
      partner3pr.map(_.strand).toRight(Intergenic),
      effect.toRight(NotAvailable),
      cosmicId.toRight(NotAvailable),
      numReads.toRight(NotAvailable)
    )
  }



  implicit val rnaSeqToView: RNASeq => RNASeqView = {
    rnaSeq =>
      RNASeqView(
        rnaSeq.patient,
        rnaSeq.entrezId,
        rnaSeq.ensemblId,
        rnaSeq.gene.symbol.toRight(NotAvailable),
        rnaSeq.transcriptId,
        rnaSeq.fragmentsPerKilobaseMillion,
        rnaSeq.fromNGS,
        rnaSeq.tissueCorrectedExpression,
        rnaSeq.rawCounts,
        rnaSeq.librarySize,
        rnaSeq.cohortRanking.toRight(NotAvailable)
      )
  }


  implicit val brcanessToDisplay: BRCAness => BRCAnessDisplay = {
    brcaness =>
      BRCAnessDisplay(s"${(brcaness.value * 100).toInt} %")
  }


  implicit val ngsReportToView: SomaticNGSReport => NGSReportView = {
    report =>
      NGSReportView(
        report.id,
        report.patient,
        report.specimen,
        report.issueDate,
        report.sequencingType,
        report.metadata,
        report.tumorCellContent.map(_.mapTo[TumorCellContentDisplay]).toRight(NotAvailable),
        report.brcaness.map(_.mapTo[BRCAnessDisplay]).toRight(NotAvailable),
        report.msi.toRight(NotAvailable),
        report.tmb.map(_.mapTo[TMBDisplay]).toRight(NotAvailable),
        report.simpleVariants.getOrElse(List.empty[SimpleVariant]).map(_.mapTo[SimpleVariantView]),
        report.copyNumberVariants.getOrElse(List.empty[CNV]).map(_.mapTo[CNVView]),
        report.dnaFusions.getOrElse(List.empty[DNAFusion]).map(_.mapTo[DNAFusionView]),
        report.rnaFusions.getOrElse(List.empty[RNAFusion]).map(_.mapTo[RNAFusionView]),
        report.rnaSeqs.getOrElse(List.empty[RNASeq]).map(_.mapTo[RNASeqView])
     )
  }



  implicit val levelOfEvidenceToDisplay: LevelOfEvidence => LevelOfEvidenceDisplay = {
    loe =>
      LevelOfEvidenceDisplay(
        s"${loe.grading.code}, Zusätze: ${loe.addendums.flatMap(_.map(_.code.toString).reduceOption(_ + ", " + _)).getOrElse("keine")}"
      )
  }


  implicit val supportingVariantToDisplay: Variant => SupportingVariantDisplay = {
    v => 
      val repr = v match {
        case snv: SimpleVariant =>
          s"SNV ${snv.gene.flatMap(_.complete.symbol.map(_.value)).getOrElse("Gene undefined")} ${snv.aminoAcidChange.map(_.code.value).getOrElse("Protein change undefined")}"
      
        case cnv: CNV => {
          val genes =
            cnv.reportedAffectedGenes
               .flatMap(_.map(_.complete.symbol.map(_.value).getOrElse("N/A")).reduceOption(_ + ", " + _))
               .getOrElse("N/A")
      
          s"CNV [${genes}] ${cnv.`type`}"
        }
      
        case DNAFusion(_,_,dom5pr,dom3pr,_) =>
          s"DNA-Fusion ${dom5pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")} :: ${dom3pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")}"
      
        case RNAFusion(_,_,dom5pr,dom3pr,_,_,_) =>
          s"RNA-Fusion ${dom5pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")} :: ${dom3pr.flatMap(_.gene.complete.symbol.map(_.value)).getOrElse("intergenic")}"
      
        case rnaSeq: RNASeq =>
          s"RNA-Seq ${rnaSeq.gene.complete.symbol.map(_.value).getOrElse("N/A")}"
      
      }

      SupportingVariantDisplay(repr)
  }


  implicit val recommendationToDisplay:
  (
   (
    TherapyRecommendation,
    Option[ICD10Display],
    Option[ECOGStatus],
    List[Variant]
   )
  ) => TherapyRecommendationView = {


    case (rec,icd10,ecog,variants) =>

      val supportingVariants = rec.supportingVariants.getOrElse(List.empty[Variant.Id])

      val (medication,medicationClasses) =
        rec.medication
          .map(_.mapTo[(MedicationDisplay,MedicationDisplay)]).unzip

      TherapyRecommendationView(
        rec.id,
        rec.patient,
        icd10.toRight(NotAvailable),
        ecog.map(_.value.mapTo[ECOGDisplay]).toRight(NotAvailable),
        medication.toRight(NotAvailable),
        medicationClasses.toRight(NotAvailable),
        rec.priority.toRight(NotAvailable),
        rec.levelOfEvidence.map(_.mapTo[LevelOfEvidenceDisplay]).toRight(NotAvailable),
        variants.filter(v => supportingVariants contains v.id).map(_.mapTo[SupportingVariantDisplay])
      )
  }


  implicit val carePlanToDisplay:
  (
   (
    (
     CarePlan,
     Diagnosis,
     List[ECOGStatus],
     List[TherapyRecommendation],
     List[StudyInclusionRequest],
     Option[GeneticCounsellingRequest]
    ),
    List[Variant]
   )
  ) => CarePlanView = {

    case ((carePlan,diagnosis,ecogs,recommendations,studyInclusionRequests,geneticCounsellingRequest),variants) =>

      val icd10 =
        diagnosis.icd10.map(_.mapTo[ICD10Display])//.toRight(NotAvailable)

      val ecog =
        carePlan.issuedOn
          .flatMap(
            d =>
              ecogs.filter(_.effectiveDate.isDefined)
                .maxByOption(_.effectiveDate.get isBefore d)
          )

      CarePlanView(
        carePlan.id,
        carePlan.patient,
        icd10.toRight(NotAvailable),
        carePlan.issuedOn.toRight(NotAvailable),
        carePlan.description.toRight(NotAvailable),
        geneticCounsellingRequest.map(_.reason).toRight(No),
        studyInclusionRequests.map(_.nctNumber.value).reduceLeftOption(_ + ", " + _).map(NCTNumbersDisplay(_)).toRight(NotAvailable),
        carePlan.noTargetFinding.map(_ => No).toRight(Yes), // NOTE: Target is available iff 'noTargetFinding' is NOT defined
        recommendations.map(rec => (rec,icd10,ecog,variants).mapTo[TherapyRecommendationView]),
        carePlan.rebiopsyRequests.toRight(NotAvailable),
      )

  }


  implicit val carePlanDataToDisplay:
  (
   (
    (
     List[CarePlan],
     List[Diagnosis],
     List[ECOGStatus],
     List[TherapyRecommendation],
     List[StudyInclusionRequest],
     List[GeneticCounsellingRequest]
    ),
    List[SomaticNGSReport]
   )
  ) => List[CarePlanView] = {

      case ((carePlans,diagnoses,ecogs,recommendations,studyInclusionReqs,geneticCounsellingReqs), ngsReports) =>

        carePlans.map {
          cp =>
            ((
              cp,
              diagnoses.find(_.id == cp.diagnosis).get,  // safe to call, because validation enforces referential integrity
              ecogs,
              cp.recommendations.fold(List.empty[TherapyRecommendation])(recs => recommendations.filter(rec => recs contains rec.id)),
              cp.studyInclusionRequests
                .map(refs => studyInclusionReqs.filter(req => refs contains (req.id))).getOrElse(List.empty),
              cp.geneticCounsellingRequest
                .flatMap(reqId => geneticCounsellingReqs.find(_.id == reqId))
             ),
             ngsReports.flatMap(_.variants)
           )
           .mapTo[CarePlanView]
        }

  }




  implicit val claimWithResponseToDisplay: ((Claim,Option[ClaimResponse])) => ClaimStatusView = {

    case (claim,response) =>

      ClaimStatusView(
        claim.id,
        claim.patient,
        claim.therapy,
        claim.issuedOn,
        response.map(_.issuedOn).toRight(NotAvailable),
        response.map(_.status)
          .flatMap(ValueSet[ClaimResponse.Status.Value].displayOf)
          .map(ClaimResponseStatusDisplay(_))
          .toRight(NotAvailable),
        response.flatMap(_.reason)
          .flatMap(ValueSet[ClaimResponse.Reason.Value].displayOf)
          .map(ClaimResponseReasonDisplay(_))
          .toRight(NotAvailable),
      )
  }

  implicit val claimsWithResponsesToDisplay: ((List[Claim],List[ClaimResponse])) => List[ClaimStatusView] = {
    case (claims,claimResponses) =>
      claims.map(
        cl => (cl,claimResponses.find(_.claim == cl.id)).mapTo[ClaimStatusView]
      )

  }



  implicit val molecularTherapyToView:
  (
   (
    MolecularTherapy,
    Option[Diagnosis],
    List[TherapyRecommendation],
    List[Variant],
    Option[Response]
   )
  ) => MolecularTherapyView = {

    case (molTh,diag,recs,variants,resp) =>

    val recommendation = recs.find(_.id == molTh.basedOn)
    val status   = ValueSet[MolecularTherapy.Status.Value].displayOf(molTh.status).get
    val note     = molTh.note.getOrElse("-")
    val icd10    = diag.flatMap(_.icd10.map(_.mapTo[ICD10Display])).toRight(NotAvailable)
    val priority = recommendation.flatMap(_.priority).toRight(NotAvailable)
    val levelOfEvidence = recommendation.flatMap(_.levelOfEvidence).map(_.grading.code).toRight(NotAvailable)

    val supportingVariants = recommendation.flatMap(_.supportingVariants).getOrElse(List.empty[Variant.Id])
    val suppVariantDisplay = variants.filter(v => supportingVariants contains v.id).map(_.mapTo[SupportingVariantDisplay])

    val response = resp.map(_.mapTo[ResponseDisplay]).toRight(NotAvailable)
    val progressionDate = resp.filter(_.value.code == RECIST.PD).map(_.effectiveDate).toRight(Undefined)

    val (medication,medicationClasses) =
      molTh.medication
        .map(_.mapTo[(MedicationDisplay,MedicationDisplay)]).unzip
 
    MolecularTherapyView(
      molTh.id,
      molTh.patient,
      icd10,
      status,
      molTh.recordedOn,
      molTh.basedOn,
      priority,
      levelOfEvidence,
      molTh.period.map(_.mapTo[PeriodDisplay[LocalDate]]).toRight(NotAvailable),
      molTh.notDoneReason.map(_.code).flatMap(ValueSet[MolecularTherapy.NotDoneReason.Value].displayOf).toRight(NotAvailable),
      medication.toRight(NotAvailable),
      medicationClasses.toRight(NotAvailable),
      suppVariantDisplay,
      molTh.reasonStopped.map(_.code).flatMap(ValueSet[MolecularTherapy.StopReason.Value].displayOf).toRight(NotAvailable),
      molTh.dosage.toRight(NotAvailable),
      note,
      response,
      progressionDate
    )
  }


  implicit val molecularTherapiesToView:
  (
   (
    List[MolecularTherapy],
    List[TherapyRecommendation],
    List[Diagnosis],
    List[SomaticNGSReport],
    List[Response]
   )
  ) => List[MolecularTherapyView] = {

    case (therapies,recommendations,diagnoses,ngsReports,responses) =>

      val diagsByRec =
        recommendations.map(rec => (rec.id,diagnoses.find(_.id == rec.diagnosis))).toMap

      val variants =
        ngsReports.flatMap(_.variants)

      therapies.map(
        th =>
          (
           th,
           diagsByRec.get(th.basedOn).flatten,
           recommendations,
           variants,
           responses.filter(_.therapy == th.id).maxByOption(_.effectiveDate)
          )
          .mapTo[MolecularTherapyView]
      )
  }



  implicit val mtbFileToView: MTBFile => MTBFileView = {

    mtbfile =>

      val diagnoses = mtbfile.diagnoses.getOrElse(List.empty[Diagnosis])

      val responses = mtbfile.responses.getOrElse(List.empty[Response])

      val ngsReports = mtbfile.ngsReports.getOrElse(List.empty[SomaticNGSReport])


      MTBFileView(
        (mtbfile.patient,
         mtbfile.consent,
         mtbfile.episode).mapTo[PatientView],

        diagnoses.map(_.mapTo[DiagnosisView]),

        mtbfile.familyMemberDiagnoses.getOrElse(List.empty)
          .map(_.mapTo[FamilyMemberDiagnosisView]),

        mtbfile.previousGuidelineTherapies.getOrElse(List.empty)
          .map(
            th =>
             (th,
              diagnoses.find(_.id == th.diagnosis),
              responses.find(_.therapy == th.id)).mapTo[GuidelineTherapyView]
          ) ++
            mtbfile.lastGuidelineTherapies.getOrElse(List.empty)
              .map(
                th =>
                  (th,
                   diagnoses.find(_.id == th.diagnosis),
                   responses.find(_.therapy == th.id)).mapTo[GuidelineTherapyView]
              ),

        mtbfile.ecogStatus
          .map((mtbfile.patient,_).mapTo[ECOGStatusView]),

        mtbfile.specimens.getOrElse(List.empty).map(_.mapTo[SpecimenView]),

        mtbfile.molecularPathologyFindings.getOrElse(List.empty)
          .map(_.mapTo[MolecularPathologyFindingView]),

        mtbfile.histologyReports.getOrElse(List.empty)
          .map(_.mapTo[HistologyReportView]),

        ngsReports.map(_.mapTo[NGSReportView]),

        (
          (mtbfile.carePlans.getOrElse(List.empty),
          mtbfile.diagnoses.getOrElse(List.empty),
          mtbfile.ecogStatus.getOrElse(List.empty),
          mtbfile.recommendations.getOrElse(List.empty),
          mtbfile.studyInclusionRequests.getOrElse(List.empty),
          mtbfile.geneticCounsellingRequests.getOrElse(List.empty)),
          ngsReports
        )
        .mapTo[List[CarePlanView]],

        (
          mtbfile.claims.getOrElse(List.empty),
          mtbfile.claimResponses.getOrElse(List.empty)
        )
        .mapTo[List[ClaimStatusView]],

        (
          mtbfile.molecularTherapies.getOrElse(List.empty)
            .filterNot(_.history.isEmpty).map(_.history.head),
          mtbfile.recommendations.getOrElse(List.empty), 
          diagnoses,
          ngsReports,
          responses 
        )
        .mapTo[List[MolecularTherapyView]]

      )

  }




}

object mappings extends mappings
{

  implicit val icd10gmCatalog = ICD10GMCatalogs.getInstance.get

  implicit val icdO3Catalog   = ICDO3Catalogs.getInstance.get

  implicit val medicationCatalog = MedicationCatalog.getInstance.get

  implicit val hgncCatalog = HGNCCatalog.getInstance.get

}
