package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.BatchState;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.exception.GeneralException;
import cz.inqool.arclib.exception.MissingObject;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import cz.inqool.arclib.store.Transactional;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.arclib.util.Utils.asList;
import static cz.inqool.arclib.util.Utils.notNull;

@Service
public class CoordinatorService {

    private SipStore sipStore;
    private BatchStore batchStore;
    private JmsTemplate template;

    /**
     * Creates and starts new batch. For each file in the specified folder creates sip package and assigns it to the batch.
     * Then it sets the state of batch to PROCESSING and sends a JMS message to Worker for each sip package.
     *
     * @param path path to the folder with files to be processed
     * @return id of the created batch
     */
    public String start(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            throw new GeneralException("The path specified does not exist!");
        }

        Set<String> sipIds = processFolder(folder);

        Batch batch = new Batch();
        batch.setIds(sipIds);
        batch.setState(BatchState.PROCESSING);
        batchStore.save(batch);

        sipIds.forEach(id -> {
            template.convertAndSend("worker", new CoordinatorDto(id, batch.getId()));
        });
        return batch.getId();
    }

    /**
     * For each file in the folder creates sip package, sets its state to PROCESSING and saves it to database.
     *
     * @param folder folder containing files to be processed
     * @return ids of the created sip packages
     */
    private Set<String> processFolder(File folder) {
        return Arrays
                .stream(folder.listFiles())
                .map(f -> {
                    Sip sip = new Sip();
                    sip.setState(SipState.NEW);
                    sip.setPath(f.getPath());
                    sipStore.save(sip);

                    return sip.getId();
                })
                .collect(Collectors.toSet());
    }

    /**
     * Cancels processing of the batch by updating its state to CANCELED.
     *
     * @param batchId id of the batch
     */
    @Transactional
    @JmsListener(destination = "cancel")
    public void cancel(String batchId) {
        Batch batch = batchStore.find(batchId);

        notNull(batch, () -> new MissingObject(Batch.class, batchId));

        batch.setState(BatchState.CANCELED);
        batchStore.save(batch);
    }

    /**
     * Suspends processing of the batch by updating its state to SUSPENDED.
     *
     * @param batchId id of the batch
     */
    @Transactional
    public void suspend(String batchId) {
        Batch batch = batchStore.find(batchId);

        notNull(batch, () -> new MissingObject(Batch.class, batchId));

        batch.setState(BatchState.SUSPENDED);
        batchStore.save(batch);
    }

    /**
     * Resumes processing of the batch.
     * a) If the batch contains any sip package with the state PROCESSING, stops the resume process and returns false.
     * b) Otherwise, updates state of the batch to PROCESSING
     * and for each sip package of the batch with the state NEW sends a JMS message to Worker.
     * In this case method returns true.
     *
     * @param batchId id of the batch
     */
    public Boolean resume(String batchId) {
        Batch batch = batchStore.find(batchId);

        notNull(batch, () -> new MissingObject(Batch.class, batchId));

        boolean hasProcessingSip = sipStore.findAllInList(asList(batch.getIds())).stream()
                .anyMatch(sip -> sip.getState() == SipState.PROCESSING);
        if (hasProcessingSip) return false;

        batch.setState(BatchState.PROCESSING);
        batchStore.save(batch);

        batch.getIds().forEach(id -> {
            Sip sip = sipStore.find(id);
            if (sip.getState() == SipState.NEW) {
                template.convertAndSend("worker", new CoordinatorDto(id, batch.getId()));
            }
        });
        return true;
    }

    @Inject
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setBatchStore(BatchStore batchStore) {
        this.batchStore = batchStore;
    }
}
