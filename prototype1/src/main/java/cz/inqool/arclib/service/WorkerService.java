package cz.inqool.arclib.service;

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
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static cz.inqool.arclib.util.Utils.*;

@Slf4j
@Service
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
     * 2. checks that the batch state is PROCESSING, updates the SIP with state PROCESSING and starts BPM process for the SIP
     *
     * @param dto object with the batch id and sip id
     * @throws InterruptedException
     */
    @Async
    @Transactional
    @JmsListener(destination = "worker")
    public void processSip(CoordinatorDto dto) throws InterruptedException {
        String sipId = dto.getSipId();
        String batchId = dto.getBatchId();

        log.info("Message received at worker. Batch ID: " + batchId + ", SIP ID: " + sipId);

        Batch batch = batchStore.find(batchId);

        notNull(batch, () -> new MissingObject(Batch.class, batchId));
        in(sipId, batch.getIds(), () -> new ForbiddenObject(Batch.class, batchId));

        if (tooManyFailedSips(batch)) {
            template.convertAndSend("cancel", batch.getId());

            log.info("Processing of batch " + batchId + " stopped because of too many SIP failures.");

            return;
        }

        if (batch.getState() == BatchState.PROCESSING) {
            Sip sip = sipStore.find(sipId);
            sip.setState(SipState.PROCESSING);
            sipStore.save(sip);

            runtimeService.startProcessInstanceByKey("Ingest", asMap("sipId", sipId)).getProcessInstanceId();
        }
    }

    /**
     * Counts the number of SIPs with the state FAILED for the given batch. If the count is bigger than 1/2 of all the SIPs of the batch,
     * sets the batch state to CANCELED and returns true, otherwise returns false.
     *
     * @param batch
     * @return
     */
    private boolean tooManyFailedSips(Batch batch) {
        int allSipsCount = batch.getIds().size();

        long failedSipsCount = sipStore.findAllInList(asList(batch.getIds())).stream()
                .filter(sip -> sip.getState() == SipState.FAILED)
                .count();

        return failedSipsCount > (allSipsCount / 2);
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