package cas.lib.arclib;

import cas.lib.arclib.exception.GeneralException;
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

public class ValidationChecker {

    /**
     * Validates XML against XSD schema
     * @param pathToXml path to the XML in which the element is being searched
     * @param xsdSchema XSD against which the XML is validated
     *
     * @throws SAXException if the XSD validation failed
     * @throws IOException if the XML at the specified path is missing
     */
    public static void validateWithXMLSchema(String pathToXml, String xsdSchema) throws IOException, SAXException {
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        InputStream stream = new ByteArrayInputStream(xsdSchema.getBytes(StandardCharsets.UTF_8));
        Schema schema;
        try {
            schema = factory.newSchema(new StreamSource(stream));
        } catch (SAXException e) {
            throw new GeneralException("XSD schema is not valid: " + xsdSchema + ".", e);
        }
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new File(pathToXml)));
    }

    /**
     * Searches XML element with XPath and returns list of nodes found
     * @param pathToXml path to the XML in which the element is being searched
     * @param expression XPath expression used in search
     * @return {@link NodeList} of elements matching the XPath in the XML
     *
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws IOException if the XML at the specified path is missing
     * @throws SAXException if the XML cannot be parsed
     * @throws ParserConfigurationException
     */
    public static NodeList findWithXPath(String pathToXml, String expression)
            throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(pathToXml);
        doc.getDocumentElement().normalize();

        XPath xPath =  XPathFactory.newInstance().newXPath();

        return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

    }

    /**
     * Checks whether the file at the path exists
     * @param path path to the file
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String path) {
        File file = new File(path);
        return file.exists();
    }
}
