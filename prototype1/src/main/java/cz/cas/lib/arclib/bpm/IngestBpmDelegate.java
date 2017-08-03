package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.SipState;
import cz.cas.lib.arclib.exception.MissingObject;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.domain.Sip;
import cz.cas.lib.arclib.exception.ForbiddenObject;
import cz.cas.lib.arclib.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cz.cas.lib.arclib.util.Utils.checked;
import static cz.cas.lib.arclib.util.Utils.notNull;
import static java.nio.file.Files.*;

@Slf4j
@Component
public class IngestBpmDelegate implements JavaDelegate {

    protected SipStore store;
    protected String workspace;

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
    public void execute(DelegateExecution execution) throws FileNotFoundException, InterruptedException {
        String sipId = (String) execution.getVariable("sipId");

        log.info("BPM process for SIP " + sipId + " started.");

        Sip sip = store.find(sipId);
        notNull(sip, () -> new MissingObject(Sip.class, sipId));

        String path = sip.getPath();
        if (path != null) {
            InputStream stream = new FileInputStream(path);

            copySipToWorkspace(sipId, stream);
            log.info("SIP " + sipId + " has been successfully copied to workspace.");

            /*
            Here will come the processing of SIP.
            We use the thread sleep to simulate the time required to process the SIP.
            */
            Thread.sleep(1000);
//            delSipFromWorkspace(sipId);
        }

        sip.setState(SipState.PROCESSED);
        store.save(sip);
        log.info("SIP " + sipId + " has been processed. The SIP state changed to PROCESSED.");
    }

    /**
     * Creates a file in workspace folder with the sip id as name and stream as file content
     *
     * @param sipId  id of the file to create
     * @param stream content of the file to create
     */
    private void copySipToWorkspace(String sipId, InputStream stream) {
        checked(() -> {
            Path folder = Paths.get(workspace);

            if (!isDirectory(folder) && exists(folder)) {
                throw new ForbiddenObject(Path.class, sipId);
            } else if (!isDirectory(folder)) {
                createDirectories(folder);
            }

            Path path = Paths.get(workspace, sipId);
            copy(stream, path);
        });
    }

    /**
     * From the workspace folder deletes file with the provided id
     *
     * @param sipId id of the file to delete
     */
    private void delSipFromWorkspace(String sipId) {
        Path path = Paths.get(workspace, sipId);

        if (exists(path)) {
            checked(() -> delete(path));
        } else {
            log.warn("File {} not found.", path);
        }
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.store = sipStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.workspace}") String workspace) {
        this.workspace = workspace;
    }
}
