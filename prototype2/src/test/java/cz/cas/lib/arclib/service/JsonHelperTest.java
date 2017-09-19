package cz.cas.lib.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JsonHelperTest {

    @Test
    public void simpleMergeTest() throws IOException {
        log.info("");
        log.info("Simple merge test: ");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode targetJson = mapper.readTree("{\n" +
                "  \"atribut1\": \"hodnota1\",\n" +
                "  \"atribut2\": \"hodnota2\"\n" +
                "}");
        log.info("Application JSON: " + targetJson);

        JsonNode sourceJson = mapper.readTree("{\n" +
                "  \"atribut2\": \"hodnota3\",\n" +
                "  \"atribut3\": \"hodnota4\"\n" +
                "}");
        log.info("Batch JSON: " + sourceJson);

        JsonNode mergedJson = JsonHelper.merge(targetJson, sourceJson);
        log.info("Result JSON: " + mergedJson);


        JsonNode expectedResult = mapper.readTree("{\"atribut1\":\"hodnota1\",\"atribut2\":\"hodnota3\"," +
                "\"atribut3\":\"hodnota4\"}}");

        assertThat(mergedJson, is(expectedResult));
    }

    @Test
    public void deepMergeTest() throws IOException {
        log.info("");
        log.info("Deep merge test: ");

        ObjectMapper mapper = new ObjectMapper();

        JsonNode targetJson = mapper.readTree("{\n" +
                "  \"atribut1\": \"hodnota1\",\n" +
                "  \"atribut2\": \"hodnota2\",\n" +
                "  \"atribut3\": {\n" +
                "    \"vnorenyAtribut1\": \"hodnota3\",\n" +
                "    \"vnorenyAtribut2\": \"hodnota4\"\n" +
                "  }\n" +
                "}");
        log.info("Application JSON: " + targetJson);

        JsonNode sourceJson = mapper.readTree("{\n" +
                "  \"atribut3\": {\n" +
                "    \"vnorenyAtribut1\": \"hodnota5\"\n" +
                "  }\n" +
                "}");
        log.info("Batch JSON: " + sourceJson);

        JsonNode mergedJson = JsonHelper.merge(targetJson, sourceJson);
        log.info("Result JSON: " + mergedJson);

        JsonNode expectedResult = mapper.readTree("{\"atribut1\":\"hodnota1\",\"atribut2\":\"hodnota2\"," +
                "\"atribut3\":{\"vnorenyAtribut1\":\"hodnota5\",\"vnorenyAtribut2\":\"hodnota4\"}}");

        assertThat(mergedJson, is(expectedResult));
    }
}
