package cas.lib.arclib;

import lombok.extern.log4j.Log4j;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Log4j
public class ValidationChecker {

    /**
     * Validates XML against XSD schema
     * @param xmlPath path to the XML in which the element is being searched
     * @param xsd XSD against which the XML is validated
     * @return true if the XML is valid, false otherwise
     */
    public static boolean validateWithXMLSchema(String xmlPath, String xsd){
        try {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            InputStream stream = new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8));
            Schema schema = factory.newSchema(new StreamSource(stream));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlPath)));
        } catch (IOException e){
            log.info("Exception: "+e.getMessage());
            return false;
        }catch(SAXException e1){
            log.info("SAX Exception: "+e1.getMessage());
            return false;
        }

        log.info("Xml validated successfully.");
        return true;
    }

    /**
     * Searches XML element with XPath
     * @param xml path to the XML in which the element is being searched
     * @param expression XPath expression used in search
     * @return NodeList of elements matching the specified XPath in the given XML
     *
     * @throws XPathExpressionException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static NodeList findWithXPath(String xml, String expression)
            throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        XPath xPath =  XPathFactory.newInstance().newXPath();

        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

        return nodeList;
    }

    /**
     * Checks whether the file at the path exists
     * @param path path to the file
     * @return true if the file exists, false otherwise
     */
    public boolean fileExistenceCheck(String path) {
        File file = new File(path);
        return file.exists();
    }
}
