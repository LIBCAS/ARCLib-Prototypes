package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QSip;
import cz.cas.lib.arclib.domain.Sip;
import cz.cas.lib.arclib.store.general.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class SipStore extends DatedStore<Sip, QSip> {
    public SipStore() {
        super(Sip.class, QSip.class);
    }
}
