package cz.inqool.arclib.bpm;

import cz.inqool.arclib.fixity.FixityCounter;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FixityBpmDelegate implements JavaDelegate {

    private FixityCounter counter;

    /**
     * Computes digest for specified file and compares it with provided digest.
     * <p>
     * Expects <i>pathToFile</i> and <i>digest</i> String variables to be set in process.
     * Sets <i>ok</i> process variable to true if digests matches, false otherwise.
     *
     * @throws IOException
     */
    @Override
    public void execute(DelegateExecution execution) throws IOException {
        Path pathToFile = Paths.get((String) execution.getVariable("pathToFile"));
        String fileDigest = (String) execution.getVariable("digest");
        execution.setVariable("ok", counter.verifyFixity(pathToFile, fileDigest));
    }

    @Inject
    public void setFixityCounter(FixityCounter counter) {
        this.counter = counter;
    }
}
