package cz.cas.lib.arclib;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import cz.cas.lib.arclib.exception.MissingNode;
import cz.cas.lib.arclib.exception.general.GeneralException;
import cz.cas.lib.arclib.service.ArclibXmlValidator;
import cz.cas.lib.arclib.service.ValidationChecker;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;

public class ArclibXmlValidationTest {

    private static String validationChecks = "arclibXmlValidationChecks.txt";

    private static String ARCLIB_XML = "arclibXml.xml";
    private static String INVALID_ARCLIB_XML_MISSING_METS_HDR = "invalidArclibXmlMissingMetsHdr.xml";
    private static String INVALID_ARCLIB_XML_INVALID_TAG = "invalidArclibXmlInvalidTag.xml";


    private static String ARCLIB_SCHEMA = "xmlSchemas/arclib.xsd";
    private static String METS_SCHEMA = "xmlSchemas/mets.xsd";
    private static String PREMIS_SCHEMA = "xmlSchemas/premis-v2-2.xsd";

    private ArclibXmlValidator validator;

    @Before
    public void setUp() {
        validator = new ArclibXmlValidator();
        validator.setArclibXmlValidationChecks(new ClassPathResource(validationChecks));
    }

    /**
     * Test of the successful scenario of the validation using the XSD validation scheme.
     */
    @Test
    public void validateArclibXmlWithSchema() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        String pathToArclibXmlSchema = Resources.getResource(ARCLIB_SCHEMA).getPath();
        String pathToXml = Resources.getResource(ARCLIB_XML).getPath();
        String pathToMetsSchema = Resources.getResource(METS_SCHEMA).getPath();
        String pathToPremisSchema = Resources.getResource(PREMIS_SCHEMA).getPath();

        FileInputStream[] xsdSchemas = new FileInputStream[]{
                new FileInputStream(pathToArclibXmlSchema),
                new FileInputStream(pathToMetsSchema),
                new FileInputStream(pathToPremisSchema)};

        ValidationChecker.validateWithXMLSchema(new FileInputStream(pathToXml), xsdSchemas);
    }

    @Test
    public void validateArclibXmlWithSchemaMissingNode() throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException {
        String pathToArclibXmlSchema = Resources.getResource(ARCLIB_SCHEMA).getPath();
        String pathToXml = Resources.getResource(INVALID_ARCLIB_XML_INVALID_TAG).getPath();
        String pathToMetsSchema = Resources.getResource(METS_SCHEMA).getPath();
        String pathToPremisSchema = Resources.getResource(PREMIS_SCHEMA).getPath();

        FileInputStream[] xsdSchemas = new FileInputStream[]{
                new FileInputStream(pathToArclibXmlSchema),
                new FileInputStream(pathToMetsSchema),
                new FileInputStream(pathToPremisSchema)};

        assertThrown(() -> ValidationChecker.validateWithXMLSchema(new FileInputStream(pathToXml), xsdSchemas))
                .isInstanceOf(GeneralException.class);
    }

    /**
     * Test of the successful scenario of the validation using the JAVA validator.
     */
    @Test
    public void validateArclibXmlWithValidator() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        URL arclibXml = Resources.getResource(ARCLIB_XML);
        validator.validateArclibXml(Resources.toString(arclibXml, Charsets.UTF_8));
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML is missing the element <i>metsHdr</i>.
     */
    @Test
    public void validateArclibXmlWithValidatorMisingNode() throws IOException, XPathExpressionException, SAXException,
            ParserConfigurationException {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_MISSING_METS_HDR);
        assertThrown(() -> validator.validateArclibXml(Resources.toString(arclibXml, Charsets.UTF_8))).isInstanceOf(MissingNode.class);
    }
}
