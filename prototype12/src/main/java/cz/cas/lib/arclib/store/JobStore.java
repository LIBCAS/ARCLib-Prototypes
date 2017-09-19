package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Job;
import cz.cas.lib.arclib.domain.QJob;
import cz.cas.lib.arclib.store.general.DatedStore;
import cz.cas.lib.arclib.store.general.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Transactional
@Repository
public class JobStore extends DatedStore<Job, QJob> {

    public JobStore() {
        super(Job.class, QJob.class);
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
