package cz.inqool.arclib;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.spring.boot.starter.test.helper.AbstractProcessEngineRuleTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;
import static org.hamcrest.Matchers.equalTo;

/**
 * Ensure the antivirus.bpmn Process is working correctly
 */
@Deployment(resources = "bpmn/antivirus.bpmn")
public class AntivirusProcessTest extends AbstractProcessEngineRuleTest {

    private static final Path QUARANTINE_FOLDER = Paths.get(System.getenv("CLAMAV"), "quarantine");
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final Path SIP = Paths.get("src/test/resources/testSIP");

    @BeforeClass
    public static void before() throws IOException {
        Files.copy(CORRUPTED_FILE_REPRESENTANT, SIP.resolve(CORRUPTED_FILE_NAME), REPLACE_EXISTING);
    }

    @AfterClass
    public static void after() throws IOException {
        Files.deleteIfExists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME));
    }

    @Test
    public void testOK() throws IOException {
        ProcessInstance process = runtimeService().startProcessInstanceByKey("antivirus",
                withVariables("pathToSip", SIP.resolve("clean.txt").toString()));
        assertThat(process).isEnded();
        Assert.assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(0L));
    }

    @Test
    public void testCorrupted() throws IOException {
        ProcessInstance process = runtimeService().startProcessInstanceByKey("antivirus",
                withVariables("pathToSip", SIP.toString()));
        assertThat(process).isEnded();
        Assert.assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(1L));
        Assert.assertThat(Files.exists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
        Assert.assertThat(Files.notExists(SIP.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
    }
}

