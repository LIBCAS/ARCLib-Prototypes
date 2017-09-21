package cz.cas.lib.arclib;

import cz.cas.lib.arclib.domain.Job;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.store.JobStore;
import cz.cas.lib.arclib.store.general.Transactional;
import cz.cas.lib.arclib.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class JobScheduler {
    private Map<String, ScheduledFuture> jobIdToScheduleFuture = new HashMap<>();
    private ThreadPoolTaskScheduler scheduler;
    private JobStore jobStore;
    private JobRunner jobRunner;

    /**
     * Schedules the job.
     *
     * @param job job to schedule
     */
    @Transactional
    public void schedule(Job job) {
        Utils.notNull(job, () -> new BadArgument("job"));

        CronTrigger trigger = new CronTrigger(job.getTiming());

        scheduler = scheduler();
        ScheduledFuture<?> future = scheduler.schedule(() -> jobRunner.run(job), trigger);
        jobIdToScheduleFuture.put(job.getId(), future);

        job.setActive(true);
        jobStore.save(job);

        log.info("Job " + job.getId() + " has been scheduled.");
    }

    /**
     * Cancels scheduling of the job.
     *
     * @param job to unschedule
     */
    @Transactional
    public void unschedule(Job job) {
        Utils.notNull(job, () -> new BadArgument("job"));

        ScheduledFuture scheduledFuture = jobIdToScheduleFuture.get(job.getId());
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            jobIdToScheduleFuture.remove(job.getId());
        }

        job.setActive(false);
        jobStore.save(job);

        log.info("Job " + job.getId() + " has been unscheduled.");
    }

    /**
     * Runs the job immediately
     *
     * @param job
     */
    @Transactional
    public void runNow(Job job) {
        jobRunner.run(job);
    }

    /**
     * Returns instance of the scheduler.
     *
     * @return instance of the scheduler
     */
    @Bean
    public ThreadPoolTaskScheduler scheduler() {
        if (scheduler == null) {
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(10);
            scheduler.afterPropertiesSet();
        }
        return scheduler;
    }

    @Inject
    public void setJobRunner(JobRunner jobRunner) {
        this.jobRunner = jobRunner;
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }
}
