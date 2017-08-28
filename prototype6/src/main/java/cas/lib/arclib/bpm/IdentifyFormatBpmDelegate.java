package cas.lib.arclib.bpm;

import cas.lib.arclib.FormatIdentifier;
import cas.lib.arclib.exception.DroidFormatIdentifierException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Component
public class IdentifyFormatBpmDelegate implements JavaDelegate {

    private FormatIdentifier formatIdentifier;

    @Override
    public void execute(DelegateExecution execution) throws InterruptedException, IOException, DroidFormatIdentifierException {
        Path pathToSip = (Path) execution.getVariable("pathToSip");

        formatIdentifier.analyze(pathToSip);
    }

    @Inject
    public void setFormatIdentifier(FormatIdentifier formatIdentifier) {
        this.formatIdentifier = formatIdentifier;
    }
}
