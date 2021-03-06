package net.surguy.maven.licensing

import java.io.File
import xml.XML

/**
 * Retrieve the license element from a POM.
 *
 * @author Inigo Surguy
 * @created 16/01/2011 22:14
 */
class LicenseExtractor {
  def retrieveLicense(pom: File):Seq[License] =
    try {
      (for (license <- XML.loadFile(pom) \\ "license")
          yield License((license \ "name" text).trim, (license \ "url" text).trim) ).toList
    } catch {
      case _ => List()
    }
}

