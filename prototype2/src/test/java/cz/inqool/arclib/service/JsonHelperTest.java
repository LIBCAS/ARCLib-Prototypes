package cz.inqool.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JsonHelperTest {
    protected JsonHelper jsonHelper;

    @Test
    public void mergeJsonObjectsTest() throws IOException {
        /*
        simple merge
         */
        ObjectMapper mapper = new ObjectMapper();
        JsonNode oldJson = mapper.readTree("{\n" +
                "  \"atribut1\": \"hodnota1\",\n" +
                "  \"atribut2\": \"hodnota2\"\n" +
                "}");

        JsonNode newJson = mapper.readTree("{\n" +
                "  \"atribut2\": \"hodnota3\",\n" +
                "  \"atribut3\": \"hodnota4\"\n" +
                "}");

        JsonNode mergedJson = JsonHelper.merge(oldJson, newJson);

        JsonNode expectedResult = mapper.readTree("{\"atribut1\":\"hodnota1\",\"atribut2\":\"hodnota3\"," +
                "\"atribut3\":\"hodnota4\"}}");

        assertThat(mergedJson, is(expectedResult));

        /*
        deep merge (nested attributes)
         */
        oldJson = mapper.readTree("{\n" +
                "  \"atribut1\": \"hodnota1\",\n" +
                "  \"atribut2\": \"hodnota2\",\n" +
                "  \"atribut3\": {\n" +
                "    \"vnorenyAtribut1\": \"hodnota3\",\n" +
                "    \"vnorenyAtribut2\": \"hodnota4\"\n" +
                "  }\n" +
                "}");

        newJson = mapper.readTree("{\n" +
                "  \"atribut2\": \"hodnota2Profil\"\n" +
                "}");

        mergedJson = JsonHelper.merge(oldJson, newJson);

        oldJson = mergedJson;
        newJson = mapper.readTree("{\n" +
                "  \"atribut3\": {\n" +
                "    \"vnorenyAtribut1\": \"hodnota3Davka\"\n" +
                "  }\n" +
                "}");

        mergedJson = JsonHelper.merge(oldJson, newJson);

        expectedResult = mapper.readTree("{\"atribut1\":\"hodnota1\",\"atribut2\":\"hodnota2Profil\"," +
                "\"atribut3\":{\"vnorenyAtribut1\":\"hodnota3Davka\",\"vnorenyAtribut2\":\"hodnota4\"}}");

        assertThat(mergedJson, is(expectedResult));
    }
}
