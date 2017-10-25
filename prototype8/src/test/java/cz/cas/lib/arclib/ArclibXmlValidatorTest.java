package cz.cas.lib.arclib;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import cz.cas.lib.arclib.exception.MissingNode;
import cz.cas.lib.arclib.service.ArclibXmlValidator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;

public class ArclibXmlValidatorTest {

    private static String VALIDATION_CHECKS = "arclibXmlValidationChecks.txt";

    private static String ARCLIB_XML = "testData/arclibXmls/arclibXml.xml";
    private static String INVALID_ARCLIB_XML_MISSING_METS_HDR = "testData/arclibXmls/invalidArclibXmlMissingMetsHdr.xml";
    private static String INVALID_ARCLIB_XML_INVALID_TAG = "testData/arclibXmls/invalidArclibXmlInvalidTag.xml";


    private static String ARCLIB_SCHEMA = "xmlSchemas/arclib.xsd";
    private static String METS_SCHEMA = "xmlSchemas/mets.xsd";
    private static String PREMIS_SCHEMA = "xmlSchemas/premis-v2-2.xsd";

    private ArclibXmlValidator validator;

    @Before
    public void setUp() {
        validator = new ArclibXmlValidator();

        validator.setArclibXmlValidationChecks(new ClassPathResource(VALIDATION_CHECKS));
        validator.setArclibXmlSchema(new ClassPathResource(ARCLIB_SCHEMA));
        validator.setMetsSchema(new ClassPathResource(METS_SCHEMA));
        validator.setPremisSchema(new ClassPathResource(PREMIS_SCHEMA));
    }

    @Test
    public void validateArclibXmlSuccess() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        URL arclibXml = Resources.getResource(ARCLIB_XML);
        validator.validateArclibXml(Resources.toString(arclibXml, Charsets.UTF_8));
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML is missing the element <i>metsHdr</i>.
     */
    @Test
    public void validateArclibXmlWithValidatorMissingNode() throws IOException, XPathExpressionException, SAXException,
            ParserConfigurationException {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_MISSING_METS_HDR);
        assertThrown(() -> validator.validateArclibXml(Resources.toString(arclibXml, Charsets.UTF_8))).isInstanceOf(MissingNode.class);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML contains an invalid tag
     */
    @Test
    public void validateArclibXmlWithValidatorInvalidTag() throws IOException, XPathExpressionException, SAXException,
            ParserConfigurationException {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_INVALID_TAG);
        assertThrown(() -> validator.validateArclibXml(Resources.toString(arclibXml, Charsets.UTF_8))).isInstanceOf(SAXException.class);
    }
}
