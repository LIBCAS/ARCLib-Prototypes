package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.QBatch;
import org.springframework.stereotype.Repository;

@Repository
public class BatchStore extends DatedStore<Batch, QBatch> {
    public BatchStore() {
        super(Batch.class, QBatch.class);
    }
}
