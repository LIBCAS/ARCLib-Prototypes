package cz.inqool.arclib;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Ensure the antivirus.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AntivirusProcessTest {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;

    private static final Path QUARANTINE_FOLDER = Paths.get(System.getenv("CLAMAV"), "quarantine");
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final Path SIP = Paths.get("src/test/resources/testSIP");

    @BeforeClass
    public static void beforeClass() throws IOException {
        Files.copy(CORRUPTED_FILE_REPRESENTANT, SIP.resolve(CORRUPTED_FILE_NAME), REPLACE_EXISTING);
    }

    @AfterClass
    public static void afterClass() throws IOException {
        Files.deleteIfExists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME));
    }

    @Before
    public void before() {
        repositoryService.createDeployment()
                .addClasspathResource("bpmn/antivirus.bpmn")
                .deploy();
    }

    /**
     * Called on single file. Tests that:
     * <ul>
     *     <li>AV scan process does not evaluate clean file as corrupted and therefore does not move it to quarantine folder</li>
     *     <li>BPM process starts scan based on given process variable (path to file)</li>
     *     <li>after scan BPM 'scan' tasks set process variable with path to corrupted files which is empty list</li>
     *     <li>BPM 'quarantine' task does not move any files to quarantine because process variable with corrupted files is </li>
     * </ul>
     */
    @Test
    public void testOK() throws IOException {
        Map variables = new HashMap();
        variables.put("pathToSip", SIP.resolve("clean.txt").toString());
        runtimeService.startProcessInstanceByKey("antivirus", variables).getId();
        assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(0L));
    }

    /**
     * Called on folder. Tests that:
     * <ul>
     *     <li>AV scan process recognizes corrupted file inside folder and move it to quarantine folder</li>
     *     <li>BPM process starts scan based on given process variable (path to file)</li>
     *     <li>after scan BPM 'scan' tasks set process variable with paths to corrupted files</li>
     *     <li>BPM 'quarantine' task moves corrupted files to quarantine based on the variable set by 'scan' task</li>
     * </ul>
     */
    @Test
    public void testCorrupted() throws IOException {
        Map variables = new HashMap();
        variables.put("pathToSip", SIP.toString());
        runtimeService.startProcessInstanceByKey("antivirus", variables).getId();
        assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(1L));
        assertThat(Files.exists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
        assertThat(Files.notExists(SIP.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
    }
}

