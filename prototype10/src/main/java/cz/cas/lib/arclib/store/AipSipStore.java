package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.QAipSip;
import org.springframework.stereotype.Repository;

@Repository
public class AipSipStore extends DomainStore<AipSip, QAipSip> {
    public AipSipStore() {
        super(AipSip.class, QAipSip.class);
    }
}
