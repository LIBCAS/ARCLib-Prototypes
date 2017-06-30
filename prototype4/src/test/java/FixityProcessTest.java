import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.spring.boot.starter.test.helper.AbstractProcessEngineRuleTest;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

/**
 * Ensure the fixity.bpmn Process is working correctly
 */
@Deployment(resources = "bpmn/fixity.bpmn")
public class FixityProcessTest extends AbstractProcessEngineRuleTest {

    private static final String DIGEST = "6f1ed002ab5595859014ebf0951522d9";
    private static final Path PATH_TO_FILE = Paths.get("src/test/resources/sample.txt");

    @Test
    public void testOK() {
        ProcessInstance process = runtimeService().startProcessInstanceByKey("fixity",
                withVariables("pathToFile", PATH_TO_FILE.toString(), "digest", DIGEST));
        assertThat(process).isEnded();
        assertThat(historyService().createHistoricVariableInstanceQuery().variableName("ok").singleResult().getValue()).isEqualTo(true);
    }

    @Test
    public void testCorrupted() {
        String corruptedDigest = DIGEST.substring(1) + "1";
        ProcessInstance process = runtimeService().startProcessInstanceByKey("fixity",
                withVariables("pathToFile", PATH_TO_FILE.toString(), "digest", corruptedDigest));
        assertThat(process).isEnded();
        assertThat(historyService().createHistoricVariableInstanceQuery().variableName("ok").singleResult().getValue()).isEqualTo(false);
    }
}