package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.Job;
import cz.inqool.arclib.domain.QJob;
import org.springframework.stereotype.Repository;

import java.util.List;

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
