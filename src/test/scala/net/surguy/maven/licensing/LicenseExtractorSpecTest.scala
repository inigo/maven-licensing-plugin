package net.surguy.maven.licensing

import java.io.File
import scala.collection.JavaConversions._
import org.specs._

/**
 * Check that licenses can be read from a file.
 *
 * @author Inigo Surguy
 * @created 16/01/2011 17:29
 */
class LicenseExtractorSpecTest extends SpecificationWithJUnit {
  val extractor = new LicenseExtractor()

  "reading licenses from file" should {
    "return an empty list when there are no licenses" in {
      licensesFor("/no-license.xml") must haveSize(0)
    }
    "return name and value for a single license" in {
      licensesFor("/apache2.xml") must haveTheSameElementsAs(
        List(License("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0")))
    }
    "return name and value when the namespace is not specified in the POM" in {
      licensesFor("/apache2.xml") must haveTheSameElementsAs(
        List(License("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0")))
    }
    "return all results when there is more than one license" in {
      licensesFor("/apache2+gpl3.xml") must haveTheSameElementsAs(
        List(License("The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0"),
             License("GNU General Public License (GPL) v3", "http://www.gnu.org/licenses/gpl.html")
        ))
    }
  }

  def licensesFor(fileName: String) = extractor.retrieveLicense(getFile(fileName))
  def getFile(resource: String) = new File(this.getClass().getResource(resource).getFile().replaceAll("%20", " "))
}