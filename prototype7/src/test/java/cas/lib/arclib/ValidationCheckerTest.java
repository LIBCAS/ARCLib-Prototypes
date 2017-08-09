package cas.lib.arclib;

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
    public void validationSchemeCheckTest() throws IOException {
        URL url = getClass().getResource("/validationProfileScheme.xsd");
        String xml = Resources.toString(url, Charsets.UTF_8);

        boolean success = validationChecker.validateWithXMLSchema(
                getClass().getResource("/testValidationProfile.xml").getPath(), xml);

        assertThat(success, is(true));
    }

    @Test
    public void xPathCheckExistentElementTest() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        NodeList nodeList = validationChecker.findWithXPath(getClass().getResource("/testValidationProfile.xml").getPath(),
                "/profile/rule");

        assertThat(nodeList.getLength(), is(4));

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node nNode = nodeList.item(i);
            assertThat(nNode.getNodeName(), is("rule"));
        }
    }

    @Test
    public void xPathCheckNonExistentElementTest() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException {
        NodeList nodeList = validationChecker.findWithXPath(getClass().getResource("/testValidationProfile.xml").getPath(),
                "/profile/nonExistentTag");

        assertThat(nodeList.getLength(), is(0));
    }

    @Test
    public void filePresenceCheckExistentFileTest() {
        boolean success = validationChecker.fileExistenceCheck(getClass().getResource("/KPW01169310/METS_KPW01169310.xml").getPath());
        assertThat(success, is(true));
    }

    @Test
    public void filePresenceCheckNonExistentFileTest() {
        boolean success = validationChecker.fileExistenceCheck("/nonExistentPath");
        assertThat(success, is(false));
    }
}
