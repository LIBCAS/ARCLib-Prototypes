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
    public void generateArclibXmlAttributeMapping() throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/simpleMappings/sipProfileAttributeMapping.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<mets LABEL=\"Z dějin malenovického hradu , [1953]\"/>"));
    }

    @Test
    public void generateArclibXmlMultipleElementsMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/simpleMappings/sipProfileMultipleElementsMapping.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mets><metsHdr><agent><METS:name>Exon s.r.o" +
                ".</METS:name>\r\n</agent><agent><METS:name>ZLG001</METS:name>\r\n</agent></metsHdr></mets>"));
    }

    @Test
    public void generateArclibXmlElementAtPositionMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/simpleMappings/sipProfileElementAtPositionMapping.xml"), StandardCharsets
                .UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mets><metsHdr><METS:agent ROLE=\"CREATOR\" TYPE=\"ORGANIZATION\"> \r\n" +
                "\t\t\t<METS:name>Exon s.r.o.</METS:name>\r\n" +
                "\t\t</METS:agent>\r\n" +
                "</metsHdr></mets>"));
    }

    @Test
    public void generateArclibXmlNestedElementMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/simpleMappings/sipProfileNestedElementMapping.xml"), StandardCharsets
                .UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<mets><METS:metsHdr CREATEDATE=\"2013-01-22T10:55:20Z\" ID=\"kpw01169310\" LASTMODDATE=\"2013-01-22T10:55:20Z\" RECORDSTATUS=\"COMPLETE\">\r\n" +
                        "\t\t<METS:agent ROLE=\"CREATOR\" TYPE=\"ORGANIZATION\"> \r\n" +
                        "\t\t\t<METS:name>Exon s.r.o.</METS:name>\r\n" +
                        "\t\t</METS:agent>\r\n" +
                        "\t\t<METS:agent ROLE=\"ARCHIVIST\" TYPE=\"ORGANIZATION\"> \r\n" +
                        "\t\t\t<METS:name>ZLG001</METS:name>\r\n" +
                        "\t\t</METS:agent>\r\n" +
                        "\t</METS:metsHdr>\r\n" +
                        "</mets>"
        ));
    }

    @Test
    public void generateArclibXmlMultipleMappings() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/sipProfileMultipleMappings.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mets LABEL=\"Z dějin malenovického hradu , [1953]\"><amdSec><techMD><mdWrap><xmlData><devices><format><fileFormat><fileFormat>image/tiff<fileFormat/><fileCount>8<fileCount/></fileFormat></format></devices></xmlData></mdWrap><mdWrap><xmlData><devices><format><fileFormat><fileFormat>image/jp2<fileFormat/><fileCount>8<fileCount/></fileFormat></format></devices></xmlData></mdWrap></techMD></amdSec><METS:metsHdr CREATEDATE=\"2013-01-22T10:55:20Z\" ID=\"kpw01169310\">\r\n" +
                "<METS:agent ROLE=\"CREATOR\" TYPE=\"INDIVIDUAL\"> \r\n" +
                "<METS:name>Administrator</METS:name>\r\n" +
                "</METS:agent>\r\n" +
                "</METS:metsHdr>\r\n" +
                "</mets>"));
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

        assertThrown(() -> generator.generateArclibXml("%@#@%@!", profile.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void generateArclibInvalidXPath() throws IOException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/simpleMappings/sipProfileInvalidXPath.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        assertThrown(() -> generator.generateArclibXml(SIP_PATH, profile.getId())).isInstanceOf(InvalidXPathException.class);
    }

    @Test
    public void generateArclibFormatCount() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/aggregationMappings/sipProfileFormatCount.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mets><amdSec><techMD><mdWrap><xmlData><devices><format><fileFormat><fileFormat>image/tiff<fileFormat/><fileCount>8<fileCount/></fileFormat></format></devices></xmlData></mdWrap><mdWrap><xmlData><devices><format><fileFormat><fileFormat>image/jp2<fileFormat/><fileCount>8<fileCount/></fileFormat></format></devices></xmlData></mdWrap></techMD></amdSec></mets>"));
    }

    @Test
    public void generateArclibDeviceCount() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/aggregationMappings/sipProfileDeviceCount.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mets><amdSec><techMD><mdWrap><xmlData><devices><device><deviceId>321008<deviceId/><fileCount>8<fileCount/></device></devices></xmlData></mdWrap></techMD></amdSec></mets>"));
    }

    @Test
    public void generateArclibEventCount() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/testData/sipProfiles/aggregationMappings/sipProfileEventCount.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<mets><amdSec><digiprovMD><mdWrap><xmlData><eventAgents><eventAgent><date>2012-10-02T14:55:07Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:06Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:08Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:34Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:43Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:44Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:47Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:48Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:45Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:46Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:33Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:32Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:31Z<date/><agentName>Recognition Server<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:07Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:06Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:08Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:34Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:43Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:44Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:47Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:48Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:45Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:46Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:33Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:32Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:31Z<date/><agentName>CopiBook RGB+<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:07Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:06Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:08Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:34Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:43Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:44Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:47Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:48Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:45Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:46Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>64<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:33Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:32Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:31Z<date/><agentName>BookRestorer<agentName/><eventType>migration<eventType/><eventCount>192<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:07Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:06Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:08Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:34Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:43Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:44Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:47Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:48Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:45Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:46Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:33Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:32Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:31Z<date/><agentName>Recognition Server<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:07Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:06Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:08Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:34Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:43Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:44Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:47Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:48Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:45Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:46Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:33Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:32Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:31Z<date/><agentName>CopiBook RGB+<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:07Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:06Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:55:08Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:34Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:43Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:44Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:47Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:48Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:45Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2012-10-02T14:58:46Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>128<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:33Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:32Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>256<eventCount/></eventAgent><eventAgent><date>2013-01-11T14:17:31Z<date/><agentName>BookRestorer<agentName/><eventType>capture<eventType/><eventCount>384<eventCount/></eventAgent></eventAgents></xmlData></mdWrap></digiprovMD></amdSec></mets>"));
    }
}
