package cz.cas.lib.arclib;

import com.google.common.io.Resources;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domain.SipProfile;
import cz.cas.lib.arclib.exception.general.MissingObject;
import cz.cas.lib.arclib.helper.DbTest;
import cz.cas.lib.arclib.service.ArclibXmlGenerator;
import cz.cas.lib.arclib.store.SipProfileStore;
import org.dom4j.InvalidXPathException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArclibXmlGeneratorTest extends DbTest {

    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "../SIP_packages/" + SIP_ID;

    private ArclibXmlGenerator generator;
    private SipProfileStore store;

    @Before
    public void setUp() {
        store = new SipProfileStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        generator = new ArclibXmlGenerator();
        generator.setStore(store);
    }

    @Test
    public void generateArclibXmlAttributeMapping() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException, TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileAttributeMapping.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<arclibXml LABEL=\"Z dějin malenovického hradu , [1953]\"/>"));
    }

    @Test
    public void generateArclibXmlMultipleElementsMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileMultipleElementsMapping.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<arclibXml><mets><metsHdr><agent><METS:name>Exon s.r.o.</METS:name>\r\n</agent><agent><METS:name>ZLG001</METS:name>\r\n</agent></metsHdr></mets></arclibXml>"));
    }

    @Test
    public void generateArclibXmlElementAtPositionMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileElementAtPositionMapping.xml"), StandardCharsets
                .UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<arclibXml><mets><metsHdr><METS:agent ROLE=\"CREATOR\" TYPE=\"ORGANIZATION\"> \r\n\t\t\t<METS:name>Exon s.r.o.</METS:name>\r\n\t\t</METS:agent>\r\n</metsHdr></mets></arclibXml>"));
    }

    @Test
    public void generateArclibXmlNestedElementMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileNestedElementMapping.xml"), StandardCharsets
                .UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<arclibXml><mets><METS:metsHdr CREATEDATE=\"2013-01-22T10:55:20Z\" ID=\"kpw01169310\" LASTMODDATE=\"2013-01-22T10:55:20Z\" RECORDSTATUS=\"COMPLETE\">\r\n\t\t<METS:agent ROLE=\"CREATOR\" TYPE=\"ORGANIZATION\"> \r\n\t\t\t<METS:name>Exon s.r.o.</METS:name>\r\n\t\t</METS:agent>\r\n\t\t<METS:agent ROLE=\"ARCHIVIST\" TYPE=\"ORGANIZATION\"> \r\n\t\t\t<METS:name>ZLG001</METS:name>\r\n\t\t</METS:agent>\r\n\t</METS:metsHdr>\r\n</mets></arclibXml>"

        ));
    }

    @Test
    public void generateArclibXmlMultipleMappings() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileMultipleMappings.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<arclibXml LABEL=\"Z dějin malenovického hradu , [1953]\"><mets><METS:metsHdr CREATEDATE=\"2013-01-22T10:55:20Z\" ID=\"kpw01169310\">\r\n<METS:agent ROLE=\"CREATOR\" TYPE=\"INDIVIDUAL\"> \r\n<METS:name>Administrator</METS:name>\r\n</METS:agent>\r\n</METS:metsHdr>\r\n</mets></arclibXml>"));
    }

    @Test
    public void generateArclibXmlMissingFile() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileMissingFile.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        assertThrown(() -> generator.generateArclibXml(SIP_PATH, profile.getId())).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void generateArclibXmlNonExistentProfile() {
        assertThrown(() -> generator.generateArclibXml(SIP_PATH, "A%#$@")).isInstanceOf(MissingObject.class);
    }

    @Test
    public void generateArclibXmlNonExistentSip() throws IOException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileMissingFile.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        assertThrown(() -> generator.generateArclibXml("%@#@%@!", profile.getId())).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void generateArclibInvalidXPath() throws IOException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource("/sipProfileInvalidXPath.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        assertThrown(() -> generator.generateArclibXml(SIP_PATH, profile.getId())).isInstanceOf(InvalidXPathException.class);
    }
}
