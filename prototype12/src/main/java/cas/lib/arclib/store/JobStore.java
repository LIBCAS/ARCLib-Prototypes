package cas.lib.arclib.store;

import cas.lib.arclib.domain.Job;
import cas.lib.arclib.domain.QJob;
import cz.inqool.uas.store.DatedStore;
import cz.inqool.uas.store.Transactional;
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
