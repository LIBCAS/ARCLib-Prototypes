package cz.cas.lib.arclib.test;

import cz.cas.lib.arclib.domain.ValidationProfile;
import cz.cas.lib.arclib.exception.InvalidNodeValue;
import cz.cas.lib.arclib.exception.MissingFile;
import cz.cas.lib.arclib.exception.SchemaValidationError;
import cz.cas.lib.arclib.exception.WrongNodeValue;
import cz.cas.lib.arclib.service.ValidationService;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.test.helper.DbTest;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.exception.general.MissingObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static cz.cas.lib.arclib.test.helper.ThrowableAssertion.assertThrown;

public class ValidationServiceTest extends DbTest {

    private ValidationService service;
    private ValidationProfileStore store;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        store = new ValidationProfileStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        service = new ValidationService();
        service.setValidationProfileStore(store);
    }

    @Test
    public void validateSipMixedChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileMixedChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        service.validateSip(sipPath, validationProfile.getId());
    }

    @Test
    public void validateSipProfileMissing() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        String sipPath = "../KPW01169310";
        assertThrown(() -> service.validateSip(sipPath, "nonExistentId")).isInstanceOf(MissingObject.class);
    }

    @Test
    public void validateSipFileExistenceChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileFileExistenceChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        service.validateSip(sipPath, validationProfile.getId());
    }

    @Test
    public void validateSipFileExistenceCheckMissingFile() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileMissingFile.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        assertThrown(() -> service.validateSip(sipPath, validationProfile.getId())).isInstanceOf(MissingFile.class);
    }

    @Test
    public void validateSipValidationSchemaChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileValidationSchemaChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        service.validateSip(sipPath, validationProfile.getId());
    }

    @Test
    public void validateSipValidationSchemaCheckFailure() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileInvalidSchema.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        assertThrown(() -> service.validateSip(sipPath, validationProfile.getId())).isInstanceOf(SchemaValidationError.class);
    }

    @Test
    public void validateSipNodeValueChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileNodeValueChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        service.validateSip(sipPath, validationProfile.getId());
    }

    @Test
    public void validateSipWrongNodeValue() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileWrongNodeValue.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        assertThrown(() -> service.validateSip(sipPath, validationProfile.getId())).isInstanceOf(WrongNodeValue.class);
    }

    @Test
    public void validateSipInvalidNodeValue() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validationProfileInvalidNodeValue.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = "../KPW01169310";
        assertThrown(() -> service.validateSip(sipPath, validationProfile.getId())).isInstanceOf(InvalidNodeValue.class);
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
