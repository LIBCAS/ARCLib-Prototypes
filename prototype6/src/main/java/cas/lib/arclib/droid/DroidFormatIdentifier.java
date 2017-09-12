package cas.lib.arclib.droid;

import cas.lib.arclib.FormatIdentifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DroidFormatIdentifier implements FormatIdentifier {

    private static final String CMD = "droid";

    @Getter
    private String workspace;

    @Override
    public Map<String, List<String>> analyze(String sipId) throws IOException, InterruptedException {
        Path workSpacePath = Paths.get(workspace);
        Path sipPath = workSpacePath.resolve(sipId);

        Path profileResultsPath = workSpacePath.resolve(sipId + ".droid");
        runProfile(sipPath, profileResultsPath);

        Path exportResultsPath = workSpacePath.resolve(sipId + ".csv");
        exportProfile(profileResultsPath, exportResultsPath);

        return parseResults(exportResultsPath, CsvResultColumn.FORMAT_NAME);
    }

    /**
     * Runs DROID that creates and runs a new profile from the files belonging the SIP
     *
     * @param pathToSIP path to the SIP to analyze
     * @param pathToResult path to the <i>.DROID</i> file with the result of the profile
     * @throws IOException
     * @throws InterruptedException
     */
    protected void runProfile(Path pathToSIP, Path pathToResult) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(CMD, "-R", "-a", pathToSIP.toAbsolutePath().toString(), "-p",
                pathToResult.toAbsolutePath().toString());
        Process p = pb.start();
        p.waitFor();
    }

    /**
     * Runs DROID that exports the results of the specified profile to a CSV file with one row for each format for each file profiled
     * (if a file has multiple identifications, then a separate row will be written out for each file and separate identification made)
     *
     * @param pathToProfile path to the <i>.DROID</i> file with the result of a profile
     * @param pathToResults path to the <i>CSV</i> file with the result of the export of profile
     * @throws IOException
     * @throws InterruptedException
     */
    protected void exportProfile(Path pathToProfile, Path pathToResults) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(CMD, "-p", pathToProfile.toAbsolutePath().toString(), "-E",
                pathToResults.toAbsolutePath().toString());
        Process p = pb.start();
        p.waitFor();
    }

    /**
     * From the CSV file with the exported profile parses the values of the specified column
     *
     * @param pathToResultsCsv path to the CSV file to parse
     * @param parsedColumn column of which respective values will appear in the result as values
     * @return map of key-value pairs where the key is the path to a file and value is the list of values that appear
     * in the same row in the parsed column
     * @throws IOException
     */
    protected Map<String, List<String>> parseResults(Path pathToResultsCsv, CsvResultColumn parsedColumn) throws IOException {
        BufferedReader br = null;
        Map<String, List<String>> filePathsToParsedColumnValues = new HashMap<>();

        final String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader(pathToResultsCsv.toAbsolutePath().toString()));
            String line = br.readLine();
            String[] header = line.split(cvsSplitBy);

            //from the first line of the CSV get the index of the the parsed column and the index of the column with file path
            int filePathColumnIndex = -1;
            int parsedColumnIndex = -1;
            for (int i = 0; (i < header.length); i++) {
                //remove double quotes
                header[i] = header[i].replace("\"", "");

                if (header[i].equals(CsvResultColumn.FILE_PATH.name())) {
                    filePathColumnIndex = i;
                }
                if (header[i].equals(parsedColumn.name())) {
                    parsedColumnIndex = i;
                }
            }

            String[] list;
            //from the following lines parse the respective column values
            while ((line = br.readLine()) != null) {
                list = line.split(cvsSplitBy);

                String parsedColumnValue = list[parsedColumnIndex].replace("\"", "").trim();
                String filePath = list[filePathColumnIndex].replace("\"", "");

                List<String> values = filePathsToParsedColumnValues.get(filePath);
                if (values == null) {
                    values = new ArrayList();
                }

                values.add(parsedColumnValue);

                filePathsToParsedColumnValues.put(filePath, values);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return filePathsToParsedColumnValues;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.workspace}") String workspace) {
        this.workspace = workspace;
    }
}
