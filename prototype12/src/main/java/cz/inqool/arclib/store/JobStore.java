package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.QJob;
import cz.inqool.arclib.index.IndexedDatedStore;
import cz.inqool.arclib.index.IndexedStore;
import org.springframework.stereotype.Repository;
import cz.inqool.arclib.domain.Job;
import cz.inqool.arclib.index.IndexedJob;

import java.util.List;

import static cz.inqool.arclib.util.Utils.toLabeledReference;

/**
 * Implementation of {@link IndexedStore} for storing {@link Job} and indexing {@link IndexedJob}.
 */
@Repository
public class JobStore extends IndexedDatedStore<Job, QJob, IndexedJob> {

    public JobStore() {
        super(Job.class, QJob.class, IndexedJob.class);
    }

    @Override
    public IndexedJob toIndexObject(Job obj) {
        IndexedJob indexed = super.toIndexObject(obj);

        indexed.setName(obj.getName());
        indexed.setTiming(obj.getTiming());
        indexed.setScriptType(toLabeledReference(obj.getScriptType()));
        indexed.setActive(obj.getActive());

        return indexed;
    }

    @Transactional
    public List<Job> findAllActive() {
        QJob qJob = qObject();

        List<Job> jobs = query().select(qJob)
                .from(qJob)
                .where(findWhereExpression())
                .where(qJob.active.eq(true))
                .fetch();

        detachAll();

        return jobs;
    }
}
