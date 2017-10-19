package cz.cas.lib.arclib;

import cz.cas.lib.arclib.api.IndexApi;
import cz.cas.lib.arclib.helper.ApiTest;
import cz.cas.lib.arclib.index.Filter;
import cz.cas.lib.arclib.index.FilterOperation;
import cz.cas.lib.arclib.solr.SolrStore;
import org.apache.commons.lang3.SystemUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static cz.cas.lib.arclib.Utils.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SolrIntegrationTest implements ApiTest {

    private static final String CMD = SystemUtils.IS_OS_WINDOWS ? "solr.cmd" : "solr";
    private static final int XML_VERSION = 11;
    private static final String TEST_FIELD_NAME = "objid";
    private static final String TEST_FIELD_VALUE = "TEST_OBJ_ID";
    private static final String ENDPOINT = "http://localhost:8983/solr/arclib_xml";

    @Inject
    private IndexApi api;

    @Value("${solr.endpoint}")
    private String endpoint;

    @Inject
    private SolrStore solrService;

    @BeforeClass
    public static void beforeClass() throws IOException, InterruptedException, SolrServerException {
        new ProcessBuilder(CMD, "stop", "-all").start().waitFor();
        Process startSolrProcess = new ProcessBuilder(CMD, "start").start();
        if (startSolrProcess.waitFor() != 0) {
            String err;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(startSolrProcess.getErrorStream()))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                err = sb.toString();
            }
            throw new IllegalStateException(String.format("Unable to start solr server: %s", err));
        }
        SolrClient client = new HttpSolrClient(ENDPOINT);
        client.deleteByQuery("*:*");
        client.commit();
    }

    @Test
    public void testCreateThenRetrieveThroughApi() throws Exception {
        String arclibXml = new String(Files.readAllBytes(Paths.get("src/test/resources/arclibXml.xml")), StandardCharsets.UTF_8);

        solrService.createIndex(UUID.randomUUID().toString(), XML_VERSION, arclibXml);
        List<String> retrievedIds = solrService.findAll(asList(new Filter(TEST_FIELD_NAME, FilterOperation.EQ, TEST_FIELD_VALUE, asList())));
        assertThat(retrievedIds, hasSize(1));

        String sipId = UUID.randomUUID().toString();
        solrService.createIndex(sipId, XML_VERSION, arclibXml);
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "STARTWITH")
                .param("filter[0].value", sipId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(sipId + "_" + XML_VERSION))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void testApiQueryUndefinedField() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "blah")
                .param("filter[0].operation", "STARTWITH")
                .param("filter[0].value", "someid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testApiQueryUndefinedOperation() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "blah")
                .param("filter[0].value", "someid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateIndexInvalidFieldType() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "blah")
                .param("filter[0].value", "someid"))
                .andExpect(status().isBadRequest());
    }

}
