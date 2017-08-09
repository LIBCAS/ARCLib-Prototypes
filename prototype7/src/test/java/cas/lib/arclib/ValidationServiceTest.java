package cas.lib.arclib;

import cas.lib.arclib.domain.ValidationProfile;
import cas.lib.arclib.service.ValidationService;
import cas.lib.arclib.store.ValidationProfileStore;
import cas.lib.arclib.test.DbTest;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ValidationServiceTest extends DbTest{

    private ValidationService service;
    private ValidationProfileStore store;
    private ValidationChecker validationChecker;

    @Mock
    private ElasticsearchTemplate template;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        store = new ValidationProfileStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(template);

        validationChecker = new ValidationChecker();

        service = new ValidationService();
        service.setValidationProfileStore(store);
        service.setValidationChecker(validationChecker);
    }

    @Test
    public void validateSipTest() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        Class<ValidationServiceTest> clazz = ValidationServiceTest.class;

        InputStream inputStream = clazz.getResourceAsStream("/testValidationProfile.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        String sipPath = getClass().getResource("/KPW01169310").getPath();
        boolean success = service.validateSip(sipPath, validationProfile.getId());
        assertThat(success, is(true));
    }

    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
