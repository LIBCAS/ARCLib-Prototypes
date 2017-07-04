package cz.inqool.arclib;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.*;
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

    @Test
    public void testOK() throws IOException {
        Map variables = new HashMap();
        variables.put("pathToSip", SIP.resolve("clean.txt").toString());
        runtimeService.startProcessInstanceByKey("antivirus", variables).getId();
        Assert.assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(0L));
    }

    @Test
    public void testCorrupted() throws IOException {
        Map variables = new HashMap();
        variables.put("pathToSip", SIP.toString());
        runtimeService.startProcessInstanceByKey("antivirus", variables).getId();
        Assert.assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(1L));
        Assert.assertThat(Files.exists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
        Assert.assertThat(Files.notExists(SIP.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
    }
}

