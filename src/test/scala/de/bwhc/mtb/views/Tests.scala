package de.bwhc.mtb.views


import org.scalatest.flatspec.AnyFlatSpec
import de.bwhc.mtb.dtos.MTBFile
import de.ekut.tbi.generators.Gen
import de.bwhc.mtb.dto.gens._



class Tests extends AnyFlatSpec
{

  import mappings._

  implicit val rnd = new scala.util.Random(42)


  "MTBFile" must "have been correctly mapped to MTBFileView" in {

    val mtbfile = Gen.of[MTBFile].next

    val view = mtbfile.mapTo[MTBFileView]

  }


}
