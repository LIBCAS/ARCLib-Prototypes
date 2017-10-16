package cz.cas.lib.arclib.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface IndexStore {

    void createIndex(String sipId, int xmlVersion, String arclibXml);

    List<String> findAll(List<Filter> filter);

    default Set<IndexFieldConfig> getFieldsConfig() throws IOException {
        List<String> fieldsDefinitions = Files.readAllLines(Paths.get("src/test/resources/fieldDefinitions.csv"));
        Set<IndexFieldConfig> fieldConfigs = new HashSet<>();
        for (String line :
                fieldsDefinitions) {
            String arr[] = line.split(",");
            fieldConfigs.add(new IndexFieldConfig(arr[0], arr[1], arr.length == 2 ? false : "full".equals(arr[2])));
        }
        return fieldConfigs;
    }
}
