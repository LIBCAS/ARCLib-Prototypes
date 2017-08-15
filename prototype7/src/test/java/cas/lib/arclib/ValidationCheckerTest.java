package cas.lib.arclib;

import cas.lib.arclib.exception.GeneralException;
import cas.lib.arclib.test.helper.ThrowableAssertion;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ValidationCheckerTest {
    private ValidationChecker validationChecker;

    @Before
    public void setUp() {
        validationChecker = new ValidationChecker();
    }

    @Test
    public void validationSchemaCheckSuccess() throws IOException, SAXException {
        URL url = getClass().getResource("/validationProfileSchema");
        String xsd = Resources.toString(url, Charsets.UTF_8);

        validationChecker.validateWithXMLSchema(getClass().getResource("/validationProfileMixedChecks.xml").getPath(), xsd);
    }

    @Test
    public void validationSchemaCheckInvalidXml() throws IOException, SAXException {
        URL url = getClass().getResource("/validationProfileSchema");
        String xsd = Resources.toString(url, Charsets.UTF_8);
        ThrowableAssertion.assertThrown(() -> validationChecker.validateWithXMLSchema(getClass().getResource
                ("/validationProfileInvalidProfile.xml").getPath(), xsd)).isInstanceOf(SAXException.class);
    }

    @Test
    public void validationSchemaCheckInvalidXsd() throws IOException, SAXException {
        URL url = getClass().getResource("/schemaInvalid.xml");
        String xsd = Resources.toString(url, Charsets.UTF_8);
        ThrowableAssertion.assertThrown(() -> validationChecker.validateWithXMLSchema(getClass().getResource
                ("/validationProfileMixedChecks.xml").getPath(), xsd)).isInstanceOf(GeneralException.class);
    }

    @Test
    public void xPathCheckExistentNode() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        NodeList nodeList = validationChecker.findWithXPath(getClass().getResource("/validationProfileMixedChecks.xml").getPath(),
                "/profile/rule");

        assertThat(nodeList.getLength(), is(4));

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node nNode = nodeList.item(i);
            assertThat(nNode.getNodeName(), is("rule"));
        }
    }

    @Test
    public void xPathCheckNonexistentNodeTest() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException {
        NodeList nodeList = validationChecker.findWithXPath(getClass().getResource("/validationProfileMixedChecks.xml").getPath(),
                "/profile/nonExistentTag");

        assertThat(nodeList.getLength(), is(0));
    }

    @Test
    public void filePresenceCheckExistentFileTest() {
        boolean success = validationChecker.fileExists(getClass().getResource("/KPW01169310/METS_KPW01169310.xml").getPath());
        assertThat(success, is(true));
    }

    @Test
    public void filePresenceCheckNonExistentFileTest() {
        boolean success = validationChecker.fileExists("/nonExistentPath");
        assertThat(success, is(false));
    }
}
