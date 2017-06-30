package cz.inqool.arclib.bpm;

import cz.inqool.arclib.SIPAntivirusScanner;
import cz.inqool.arclib.clamAV.ClamSIPAntivirusScanner;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class QuarantineBpmDelegate implements JavaDelegate {

    protected SIPAntivirusScanner scanner;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        scanner = new ClamSIPAntivirusScanner();
        List<String> infectedFiles = (List<String>) execution.getVariable("infectedFiles");

        scanner.moveToQuarantine(infectedFiles.stream().map(
                pathString -> Paths.get(pathString)
        ).collect(Collectors.toList()));
    }
}
