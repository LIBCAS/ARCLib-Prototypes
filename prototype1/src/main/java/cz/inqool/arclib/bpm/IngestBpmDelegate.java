package cz.inqool.arclib.bpm;

import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.exception.ForbiddenObject;
import cz.inqool.arclib.exception.MissingObject;
import cz.inqool.arclib.store.SipStore;
import cz.inqool.arclib.store.Transactional;
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

import static cz.inqool.arclib.util.Utils.checked;
import static cz.inqool.arclib.util.Utils.notNull;
import static java.nio.file.Files.*;

@Slf4j
@Component
public class IngestBpmDelegate implements JavaDelegate {

    protected SipStore store;
    protected String workspace;

    /**
     * Executes the ingest process for the given SIP:
     * 1. copies SIP to workspace
     * 2. processes SIP (...waits for 3 seconds)
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

        Sip sip = store.find(sipId);
        notNull(sip, () -> new MissingObject(Sip.class, sipId));

        InputStream stream = new FileInputStream(sip.getPath());

        copySipToWorkspace(sipId, stream);

        log.info("Processing SIP " + sipId + ". Thread " + Thread.currentThread().getId() + " is putting itself to " +
                "sleep.");
        Thread.sleep(3000);

        sip.setState(SipState.PROCESSED);
        store.save(sip);
        log.info("SIP " + sipId + " has been processed.");

        delSipFromWorkspace(sipId);
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
