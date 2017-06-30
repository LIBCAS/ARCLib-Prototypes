package cz.inqool.arclib.bpm;

import cz.inqool.arclib.FixityCounter;
import cz.inqool.arclib.Md5FixityCounter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class FixityBpmDelegate implements JavaDelegate {

    protected FixityCounter counter;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Path pathToFile = Paths.get((String) execution.getVariable("pathToFile"));
        String fileDigest = (String) execution.getVariable("digest");
        counter = new Md5FixityCounter();
        execution.setVariable("ok", counter.verifyFixity(pathToFile, fileDigest));
    }

    @Inject
    public void setFixityCounter(FixityCounter counter) {
        this.counter = counter;
    }
}
