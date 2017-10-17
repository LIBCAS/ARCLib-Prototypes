package cz.cas.lib.arclib;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
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

public class ArclibXmlValidatorTest {

    private static String validationChecks = "arclibXmlValidationChecks.txt";

    private static String ARCLIB_XML = "arclibXml.xml";

    private static String ARCLIB_SCHEMA = "xmlSchemas/arclib.xsd";
    private static String METS_SCHEMA = "xmlSchemas/mets.xsd";
    private static String PREMIS_SCHEMA = "xmlSchemas/premis-v2-2.xsd";

    private ArclibXmlValidator validator;

    @Before
    public void setUp() {
        validator = new ArclibXmlValidator();
        validator.setArclibXmlValidationChecks(new ClassPathResource(validationChecks));
    }

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
    public void validateArclibXmlWithValidator() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        URL arclibXml = Resources.getResource(ARCLIB_XML);
        validator.validateArclibXml(Resources.toString(arclibXml, Charsets.UTF_8));
    }
}
