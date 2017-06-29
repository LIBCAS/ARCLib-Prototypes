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
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    private String processInstanceId;

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

    private void processSip(String sipId) throws InterruptedException {
        Map variables = new HashMap();
        variables.put("sipId", sipId);

        processInstanceId = runtimeService.startProcessInstanceByKey("Ingest", variables).getProcessInstanceId();

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        taskService.complete(task.getId());
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

    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
