package cz.inqool.arclib.bpm;

import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.BatchState;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.exception.ForbiddenObject;
import cz.inqool.arclib.exception.MissingObject;
import cz.inqool.arclib.store.BatchStore;
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

import static cz.inqool.arclib.util.Utils.asList;
import static cz.inqool.arclib.util.Utils.checked;
import static cz.inqool.arclib.util.Utils.notNull;
import static java.nio.file.Files.*;

@Slf4j
@Component
public class IngestBpmDelegate implements JavaDelegate {

    protected SipStore sipStore;
    protected BatchStore batchStore;
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
        String batchId = (String) execution.getVariable("batchId");

        log.info("BPM process for SIP " + sipId + " started.");

        try {
            Sip sip = sipStore.find(sipId);
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
        sipStore.save(sip);
        log.info("SIP " + sipId + " has been processed. The SIP state changed to PROCESSED.");

        } finally {
            Batch batch = batchStore.find(batchId);
            notNull(batch, () -> new MissingObject(Batch.class, batchId));

            boolean allSipsProcessed = sipStore.findAllInList(asList(batch.getIds())).stream()
                    .allMatch(s -> s.getState() == SipState.PROCESSED ||
                                    s.getState() == SipState.FAILED);

            if (allSipsProcessed && batch.getState() == BatchState.PROCESSING) {
                batch.setState(BatchState.PROCESSED);
                batchStore.save(batch);
                log.info("Batch " + batchId + " has been processed. The batch state changed to PROCESSED.");
            }
        }
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
        this.sipStore = sipStore;
    }

    @Inject
    public void setBatchStore(BatchStore batchStore) {
        this.batchStore = batchStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.workspace}") String workspace) {
        this.workspace = workspace;
    }
}