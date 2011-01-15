package net.surguy.maven.licensing;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * Test extracting license files from POMs.
 *
 * @author Inigo Surguy
 * @created 15/01/2011 14:36
 */
public class LicenseExtractorTest {

    private final LicenseExtractor extractor = new LicenseExtractor();

    @Test
    public void readSingleLicense() throws Exception {
        List<License> licenses = extractor.retrieveLicense(getFile("/apache2.xml"));
        assertEquals(1, licenses.size());
        assertEquals("The Apache Software License, Version 2.0", licenses.get(0).getName());
        assertEquals("http://www.apache.org/licenses/LICENSE-2.0", licenses.get(0).getUrl());
    }

    @Test
    public void readNoNamespaceLicense() throws Exception {
        List<License> licenses = extractor.retrieveLicense(getFile("/commons-logging-1.0.4.xml"));
        assertEquals(1, licenses.size());
        assertEquals("The Apache Software License, Version 2.0", licenses.get(0).getName());
        assertEquals("/LICENSE.txt", licenses.get(0).getUrl());
    }

    @Test
    public void readTwoLicenses() throws Exception {
        List<License> licenses = extractor.retrieveLicense(getFile("/apache2+gpl3.xml"));
        assertEquals(2, licenses.size());
        assertEquals("The Apache Software License, Version 2.0", licenses.get(0).getName());
        assertEquals("http://www.apache.org/licenses/LICENSE-2.0", licenses.get(0).getUrl());
        assertEquals("GNU General Public License (GPL) v3", licenses.get(1).getName());
        assertEquals("http://www.gnu.org/licenses/gpl.html", licenses.get(1).getUrl());
    }

    @Test
    public void readNoLicense() throws Exception {
        List<License> licenses = extractor.retrieveLicense(getFile("/no-license.xml"));
        assertEquals(0, licenses.size());
    }

    private File getFile(String s) {
        return new File(this.getClass().getResource(s).getFile().replaceAll("%20", " "));
    }

}
