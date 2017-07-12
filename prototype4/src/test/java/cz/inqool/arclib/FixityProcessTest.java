package cz.inqool.arclib;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


/**
 * Ensure the fixity.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FixityProcessTest {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;

    private static final String DIGEST = "6f1ed002ab5595859014ebf0951522d9";
    private static final Path PATH_TO_FILE = Paths.get("src/test/resources/sample.txt");
    private String processInstanceId = null;

    @Before
    public void before() {
        repositoryService.createDeployment()
                .addClasspathResource("bpmn/fixity.bpmn")
                .deploy();
    }

    @After
    public void after() {
        if(processInstanceId != null)
            historyService.deleteHistoricProcessInstance(processInstanceId);
    }

    @Test
    public void testOK() {
        Map variables = new HashMap();
        variables.put("pathToFile", PATH_TO_FILE.toString());
        variables.put("digest", DIGEST);
        processInstanceId = runtimeService.startProcessInstanceByKey("fixity", variables).getId();
        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("ok").singleResult().getValue(), equalTo(true));
    }

    @Test
    public void testCorrupted() {
        String corruptedDigest = DIGEST.substring(1) + "1";
        Map variables = new HashMap();
        variables.put("pathToFile", PATH_TO_FILE.toString());
        variables.put("digest", corruptedDigest);
        processInstanceId = runtimeService.startProcessInstanceByKey("fixity", variables).getId();
        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("ok").singleResult().getValue(), equalTo(false));
    }
}