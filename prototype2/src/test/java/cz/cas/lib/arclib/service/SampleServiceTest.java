package cz.cas.lib.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(SpringRunner.class)
@SpringBootTest
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
    public void computeIngestConfigBatchIngestConfigNull() throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode applicationIngestConfigJson = mapper.readTree(applicationIngestConfig.getInputStream());

        String computedIngestConfig = service.computeIngestConfig(null);
        assertThat(computedIngestConfig, is(mapper.writeValueAsString(applicationIngestConfigJson)));
    }

    @Test
    public void computeIngestConfigBatchIngestConfigProvided() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode applicationIngestConfigJson = mapper.readTree(applicationIngestConfig.getInputStream());

        String batchConfig = "{\n" +
                "  \"atribut2\": \"hodnota3\",\n" +
                "  \"atribut3\": \"hodnota4\"\n" +
                "}";
        JsonNode batchIngestConfigJson = mapper.readTree(batchConfig);
        JsonNode expectedIngestConfigJson = JsonHelper.merge(applicationIngestConfigJson, batchIngestConfigJson);

        String computedIngestConfig2 = service.computeIngestConfig(batchConfig);
        assertThat(computedIngestConfig2, is(mapper.writeValueAsString(expectedIngestConfigJson)));
    }
}
