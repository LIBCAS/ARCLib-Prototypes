package cz.inqool.arclib.bpm;

import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.store.SipStore;
import cz.inqool.arclib.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Slf4j
@Service
public class IngestBpmService {
    protected SipStore store;

    @Transactional
    public void processSip(String sipId) throws InterruptedException {
        Sip sip = store.find(sipId);
        log.info("Processing SIP " + sip.getId() + ". Thread " + Thread.currentThread().getId() + " is putting itself to " +
                "sleep.");

        Thread.sleep(3000);
        sip.setState(SipState.PROCESSED);
        store.save(sip);

        log.info("SIP " + sip.getId() + " has been processed.");
    }

    @Inject
    public void setStore(SipStore store) {
        this.store = store;
    }
}
