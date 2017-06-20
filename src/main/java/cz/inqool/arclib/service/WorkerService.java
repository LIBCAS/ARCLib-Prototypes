package cz.inqool.arclib.service;

import cz.inqool.arclib.bpm.IngestBpmService;
import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.BatchState;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.service.CoordinatorDto;
import cz.inqool.arclib.service.WorkerDto;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import cz.inqool.arclib.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Slf4j
@Component
public class WorkerService {
    private SipStore sipStore;
    private BatchStore batchStore;
    private JmsTemplate template;
    protected IngestBpmService service;

    @Transactional
    @Async
    @JmsListener(destination = "worker")
    public void onMessage(CoordinatorDto coordinatorDto) throws InterruptedException {
        log.info("Message received at worker. Entity ID: " + coordinatorDto.getBatchId() + ", SIP ID: " + coordinatorDto.getSipId());

        Batch batch = batchStore.find(coordinatorDto.getBatchId());

        if (stopAtMultipleFailures(batch)) return;

        if (batch.getState() == BatchState.PROCESSING) {
            service.processSip(coordinatorDto.getSipId());
        }
    }

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
    public void setService(IngestBpmService service) {
        this.service = service;
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
}
