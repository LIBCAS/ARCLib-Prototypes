package cas.lib.arclib.store;

import cas.lib.arclib.domain.QSip;
import cas.lib.arclib.domain.Sip;
import cz.inqool.uas.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class SipStore extends DatedStore<Sip, QSip> {
    public SipStore() {
        super(Sip.class, QSip.class);
    }
}
