package cz.inqool.arclib.bpm;

import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.store.SipStore;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Slf4j
@Component
public class IngestBpmDelegate implements JavaDelegate {

    protected SipStore store;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String sipId = (String) execution.getVariable("sipId");
        Sip sip = store.find(sipId);

        log.info("Processing SIP " + sipId + ". Thread " + Thread.currentThread().getId() + " is putting itself to " +
                "sleep.");
        Thread.sleep(3000);
        sip.setState(SipState.PROCESSED);
        store.save(sip);

        log.info("SIP " + sipId + " has been processed.");
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.store = sipStore;
    }
}
