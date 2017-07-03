package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.BatchState;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import cz.inqool.arclib.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WorkerService {
    private SipStore sipStore;
    private BatchStore batchStore;
    private JmsTemplate template;
    private RuntimeService runtimeService;

    /**
     * Receives JMS message from the coordinator and does the following:
     * <p>
     * 1. retrieves the specified batch from database, if the batch has got more than 1/2 failures in processing of its SIPs,
     * method stops evaluation, otherwise continues with the next step
     * <p>
     * 2. checks that the batch state is PROCESSING and then processes the SIP
     *
     * @param coordinatorDto object with the batch id and sip id
     * @throws InterruptedException
     */
    @Transactional
    @Async
    @JmsListener(destination = "worker")
    public void onMessage(CoordinatorDto coordinatorDto) throws InterruptedException {
        log.info("Message received at worker. Entity ID: " + coordinatorDto.getBatchId() + ", SIP ID: " + coordinatorDto.getSipId());

        Batch batch = batchStore.find(coordinatorDto.getBatchId());

        if (stopAtMultipleFailures(batch)) return;

        if (batch.getState() == BatchState.PROCESSING) {
            processSip(coordinatorDto.getSipId());
        }
    }

    /**
     * Starts the ingest BPM process for the given SIP
     *
     * @param sipId id of the SIP to run
     * @throws InterruptedException
     */
    private void processSip(String sipId) throws InterruptedException {
        Map variables = new HashMap();
        variables.put("sipId", sipId);

        runtimeService.startProcessInstanceByKey("Ingest", variables).getProcessInstanceId();
    }

    /**
     * Counts the number of SIPs with the state FAILED for the given batch. If the count is bigger than 1/2 of all the SIPs of the batch,
     * sets the batch state to CANCELED and returns true, otherwise returns false.
     *
     * @param batch
     * @return
     */
    private boolean stopAtMultipleFailures(Batch batch) {
        int allSipsCount = batch.getIds().size();
        long failedSipsCount = batch.getIds()
                .stream()
                .filter(id -> {
                    Sip sip = sipStore.find(id);
                    return (sip.getState() == SipState.FAILED);
                })
                .count();

        if (failedSipsCount > allSipsCount / 2) {
            template.convertAndSend("coordinator", new WorkerDto(batch.getId(), BatchState.CANCELED));
            return true;
        } else {
            return false;
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
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }
}
