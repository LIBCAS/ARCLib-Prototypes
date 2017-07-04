package cz.inqool.arclib.bpm;

import cz.inqool.arclib.SIPAntivirusScanner;
import cz.inqool.arclib.clamAV.ClamSIPAntivirusScanner;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ScanBpmDelegate implements JavaDelegate {

    private SIPAntivirusScanner scanner;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        scanner = new ClamSIPAntivirusScanner();
        List<Path> infectedFiles = scanner.scan((String) execution.getVariable("pathToSip"));

        execution.setVariable("infectedFiles",
                infectedFiles.stream().map(
                        filePath -> filePath.toString()
                ).collect(Collectors.toList())
        );
    }

    @Inject
    public void setScanner(SIPAntivirusScanner scanner) {
        this.scanner = scanner;
    }
}
