package cz.inqool.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SampleServiceTest {
    private SampleService service;
    private ClassPathResource applicationIngestConfig;

    @Before
    public void setUp() {
        service = new SampleService();
        applicationIngestConfig = new ClassPathResource("applicationIngestConfig.json");
        service.setApplicationIngestConfig(applicationIngestConfig);
    }

    @Test
    public void computeIngestConfigTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode applicationIngestConfigJson = mapper.readTree(applicationIngestConfig.getInputStream());

        /*
        Batch ingest config is null
         */
        String computedIngestConfig = service.computeIngestConfig(null);
        assertThat(computedIngestConfig, is(mapper.writeValueAsString(applicationIngestConfigJson)));

        /*
        Batch ingest config overrides application config
         */
        String batchConfig = "{\n" +
                "  \"atribut2\": \"hodnota3\",\n" +
                "  \"atribut3\": \"hodnota4\"\n" +
                "}";
        JsonNode batchIngestConfigJson = mapper.readTree(batchConfig);
        JsonNode expectedIngestConfigJson = JsonHelper.merge(batchIngestConfigJson, applicationIngestConfigJson);

        String computedIngestConfig2 = service.computeIngestConfig(batchConfig);
        assertThat(computedIngestConfig2, is(mapper.writeValueAsString(expectedIngestConfigJson)));
    }
}
