package net.surguy.maven.licensing

import java.io.File
import org.specs._
import tools.nsc.io.{Path, Directory}

/**
 * Check that all licenses in the local repository can be read.
 *
 * @author Inigo Surguy
 * @created 1/02/2011
 */
class ReadAllLicensesTest extends SpecificationWithJUnit {
  val extractor = new LicenseExtractor()
  val repoDir = new Directory(new File(System.getProperty("user.home")+"/.m2/repository/"))
  val allPoms = repoDir.walkFilter( d => d.isDirectory || d.name.endsWith(".pom") ).toList

  def parsePoms(poms: Seq[Path]) = poms.map( p => extractor.retrieveLicense(p.jfile) )

  "reading all licenses" should {
    "not return any errors" in {
      parsePoms(allPoms).foreach(println)
    }
  }

}