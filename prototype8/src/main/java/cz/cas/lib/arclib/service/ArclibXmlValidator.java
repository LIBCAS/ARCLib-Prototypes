package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.Utils;
import cz.cas.lib.arclib.exception.MissingNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cz.cas.lib.arclib.Utils.readLinesOfFileToList;


@Service
public class ArclibXmlValidator {

    private Resource arclibXmlValidationChecks;
    private Resource arclibXmlSchema;
    private Resource metsSchema;
    private Resource premisSchema;

    /**
     * Validates the structure of ARCLib XML.
     * 1. Validator checks presence of the given nodes in ARCLib XML according to the XPaths specified
     * in the file <i>arclibXmlValidationChecks.txt</i>.
     * 2. ARCLib XML is validated against XML schemas: <i>arclibXmlSchema, metsSchema, premisSchema</i>
     *
     * @param xml ARCLib XML to validate
     * @throws IOException                  if file with ARCLib XML validation checks does not exist
     * @throws XPathExpressionException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public void validateArclibXml(String xml) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        List<String> xPaths = readLinesOfFileToList(arclibXmlValidationChecks.getFile());
        for (String xPath : xPaths) {
            checkNodeExists(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name())), xPath);
        }

        InputStream[] xsdSchemas = new InputStream[]{
                arclibXmlSchema.getInputStream(),
                metsSchema.getInputStream(),
                premisSchema.getInputStream()};
        ArclibXmlValidator.validateWithXMLSchema(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name())), xsdSchemas);
    }

    /**
     * Checks if the node at the xPath exists in the XML. If the node does not exist, {@link MissingNode} exception is thrown.
     *
     * @param xml   xml
     * @param xPath xPath
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    private void checkNodeExists(InputStream xml, String xPath) throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException {
        NodeList withXPath = XPathUtils.findWithXPath(xml, xPath);

        Utils.ne(withXPath.getLength(), 0, () -> new MissingNode(xPath));
    }

    /**
     * Validates XML against XSD schema
     *
     * @param xml     XML in which the element is being searched
     * @param schemas XSD schemas against which the XML is validated
     * @throws SAXException if the XSD schema is invalid
     * @throws IOException  if the XML at the specified path is missing
     */
    private static void validateWithXMLSchema(InputStream xml, InputStream[] schemas) throws IOException, SAXException {
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Source[] sources = new Source[schemas.length];
        for (int i = 0; i < schemas.length; i++) {
            sources[i] = new StreamSource(schemas[i]);
        }

        Schema schema = factory.newSchema(sources);
        Validator validator = schema.newValidator();

        validator.validate(new StreamSource(xml));
    }

    @Inject
    public void setArclibXmlValidationChecks(@Value("${arclib.arclibXmlValidationChecks}") Resource arclibXmlValidationChecks) {
        this.arclibXmlValidationChecks = arclibXmlValidationChecks;
    }

    @Inject
    public void setArclibXmlSchema(@Value("${arclib.arclibXmlSchema}") Resource arclibXmlSchema) {
        this.arclibXmlSchema = arclibXmlSchema;
    }

    @Inject
    public void setMetsSchema(@Value("${arclib.metsSchema}") Resource metsSchema) {
        this.metsSchema = metsSchema;
    }

    @Inject
    public void setPremisSchema(@Value("${arclib.premisSchema}") Resource premisSchema) {
        this.premisSchema = premisSchema;
    }
}
