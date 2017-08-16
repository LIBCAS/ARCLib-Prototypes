package cas.lib.arclib.bpm;

import cas.lib.arclib.domain.Sip;
import cas.lib.arclib.domain.SipState;
import cas.lib.arclib.exception.MissingObject;
import cas.lib.arclib.service.ValidationService;
import cas.lib.arclib.store.SipStore;
import cas.lib.arclib.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cas.lib.arclib.util.Utils.checked;
import static cas.lib.arclib.util.Utils.notNull;
import static java.nio.file.Files.createDirectories;
import static org.elasticsearch.common.io.FileSystemUtils.exists;

@Slf4j
@Component
public class ValidateSipBpmDelegate implements JavaDelegate {

    protected SipStore sipStore;
    protected String workspace;
    protected ValidationService service;

    /**
     * Executes the ingest process for the given SIP:
     * 1. copies SIP to workspace
     * 2. processes SIP (...waits for a second)
     * 3. deletes SIP from workspace
     *
     * @param execution parameter containing the SIP id
     * @throws FileNotFoundException
     * @throws InterruptedException
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws IOException, InterruptedException, ParserConfigurationException,
            SAXException, XPathExpressionException {
        String sipId = (String) execution.getVariable("sipId");
        String validationProfileId = (String) execution.getVariable("validationProfileId");

        log.info("BPM process for SIP " + sipId + " started.");

        Sip sip = sipStore.find(sipId);
        notNull(sip, () -> new MissingObject(Sip.class, sipId));

        String path = sip.getPath();
        if (path != null) {
            copySipToWorkspace(path, sipId);

            log.info("SIP " + sipId + " has been successfully copied to workspace.");

            Path workspacePath = Paths.get(workspace, sipId);
            service.validateSip(workspacePath.toString(), validationProfileId);

            delSipFromWorkspace(sipId);
        }

        sip.setState(SipState.PROCESSED);
        sipStore.save(sip);
        log.info("SIP " + sipId + " has been processed. The SIP state changed to PROCESSED.");
    }

    /**
     * Creates a file in workspace folder with the sip id as name and stream as file content
     */
    private void copySipToWorkspace(String src, String sipId) throws IOException {
        checked(() -> {
            Path folder = Paths.get(workspace);
            if (!exists(folder)) {
                createDirectories(folder);
            }

            FileSystemUtils.copyRecursively(new File(src), new File(workspace + "/" + sipId));
        });
    }

    /**
     * From the workspace deletes the folder with the data for the respective SIP
     *
     * @param sipId id of the file to delete
     */
    private void delSipFromWorkspace(String sipId) {
        Path path = Paths.get(workspace, sipId);

        if (exists(path)) {
            checked(() -> FileSystemUtils.deleteRecursively(new File(workspace + "/" + sipId)));
        } else {
            log.warn("File {} not found.", path);
        }
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.workspace}") String workspace) {
        this.workspace = workspace;
    }

    @Inject
    public void setService(ValidationService service) {
        this.service = service;
    }
}
