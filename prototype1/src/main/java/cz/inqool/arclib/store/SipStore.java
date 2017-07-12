package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.QSip;
import cz.inqool.arclib.domain.Sip;
import org.springframework.stereotype.Repository;

@Repository
public class SipStore extends DomainStore<Sip, QSip> {
    public SipStore() {
        super(Sip.class, QSip.class);
    }
}
