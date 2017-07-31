package cz.inqool.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class SampleService {
    private Resource applicationIngestConfig;

    /**
     * Computes resulting ingest config by merging application ingest config and batch ingest config,
     * if there are two equally named attributes the batch ingest config has the priority over the application ingest config
     * @param batchIngestConfig ingest config provided with the given batch
     * @return computed ingest config
     * @throws IOException if the application config is inaccessible or the batch ingest config cannot be parsed
     */
    public String computeIngestConfig(String batchIngestConfig) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode ingestConfig;
        try (InputStream in = applicationIngestConfig.getInputStream()) {
            ingestConfig = mapper.readTree(in);

            if (batchIngestConfig != null) {
                JsonNode batchIngestConfigJson = mapper.readTree(batchIngestConfig);
                log.info("Batch ingest config json: " + batchIngestConfigJson);

                ingestConfig = JsonHelper.merge(batchIngestConfigJson, ingestConfig);
            } else {
                log.info("Batch ingest config json is empty.");
            }
        }
        log.info("Result ingest config json: " + ingestConfig);
        return mapper.writeValueAsString(ingestConfig);
    }

    @Inject
    public void setApplicationIngestConfig(@Value("${arclib.applicationIngestConfig}") Resource applicationIngestConfig) {
        this.applicationIngestConfig = applicationIngestConfig;
    }
}
