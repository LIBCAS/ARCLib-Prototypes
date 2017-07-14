package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.QAipSip;
import cz.inqool.uas.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class AipSipStore extends DatedStore<AipSip, QAipSip> {
    public AipSipStore() {
        super(AipSip.class, QAipSip.class);
    }
}
