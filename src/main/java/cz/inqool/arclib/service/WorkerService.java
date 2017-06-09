package cz.inqool.arclib.service;

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

    @Transactional
    @Async
    @JmsListener(destination = "worker")
    public void onMessage(CoordinatorDto coordinatorDto) throws InterruptedException {
        log.info("Message received at worker. Entity ID: " + coordinatorDto.getBatchId() + ", SIP ID: " + coordinatorDto.getSipId());

        Batch batch = batchStore.find(coordinatorDto.getBatchId());

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
            return;
        }

        if (batch.getState() == BatchState.PROCESSING) {
            Sip sip = sipStore.find(coordinatorDto.getSipId());

            log.info("Processing SIP " + sip.getId() + ". Thread " + Thread.currentThread().getId() + " is putting itself to " +
                    "sleep.");

            Thread.sleep(3000);
            sip.setState(SipState.PROCESSED);
            sipStore.save(sip);

            log.info("SIP " + sip.getId() + " has been processed.");
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
}
