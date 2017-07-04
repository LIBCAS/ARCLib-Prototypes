package cz.inqool.arclib.bpm;

import cz.inqool.arclib.SIPAntivirusScanner;
import cz.inqool.arclib.clamAV.ClamSIPAntivirusScanner;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class QuarantineBpmDelegate implements JavaDelegate {

    private SIPAntivirusScanner scanner;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        scanner = new ClamSIPAntivirusScanner();
        List<String> infectedFiles = (List<String>) execution.getVariable("infectedFiles");

        scanner.moveToQuarantine(infectedFiles.stream().map(
                pathString -> Paths.get(pathString)
        ).collect(Collectors.toList()));
    }

    @Inject
    public void setScanner(SIPAntivirusScanner scanner) {
        this.scanner = scanner;
    }
}
