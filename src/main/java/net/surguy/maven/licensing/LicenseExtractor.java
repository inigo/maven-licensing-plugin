package net.surguy.maven.licensing;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Retrieve the license element from a POM.
 *
 * @author Inigo Surguy
 * @created 15/01/2011 14:20
 */
public class LicenseExtractor {

    List<License> retrieveLicense(File pom) throws IOException, SAXException {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(pom);
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    if (prefix == null) throw new NullPointerException("Null prefix");
                    else if ("m".equals(prefix)) return "http://maven.apache.org/POM/4.0.0";
                    else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
                    return XMLConstants.NULL_NS_URI;
                }
                public String getPrefix(String s) { throw new UnsupportedOperationException("Unsupported operation"); }
                public Iterator getPrefixes(String s) { throw new UnsupportedOperationException("Unsupported operation"); }
            });
            XPathExpression expr = xpath.compile("/m:project/m:licenses/m:license | /project/licenses/license");

            List<License> licenses = new ArrayList<License>();
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                License license = toLicense(n);
                if (license != null) licenses.add(license);
            }
            return licenses;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Parser misconfigured - Java is set up incorrectly : " + e, e);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("XPath invalid : " + e, e);
        }
    }

    private License toLicense(Node n) {
        NodeList childNodes = n.getChildNodes();
        String name = null;
        String url = null;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if ("name".equals(node.getLocalName())) name = node.getTextContent();
            if ("url".equals(node.getLocalName())) url = node.getTextContent();
        }
        if (name != null || url != null) {
            return new License(name, url);
        } else {
            return null;
        }
    }

}
