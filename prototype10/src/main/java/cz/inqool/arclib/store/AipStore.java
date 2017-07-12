package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.Aip;
import cz.inqool.arclib.domain.QAip;
import cz.inqool.uas.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class AipStore extends DatedStore<Aip, QAip> {
    public AipStore() {
        super(Aip.class, QAip.class);
    }
}
