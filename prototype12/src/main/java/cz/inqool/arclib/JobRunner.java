package cz.inqool.arclib;

import cz.inqool.arclib.domain.Job;
import cz.inqool.arclib.exception.BadArgument;
import cz.inqool.arclib.exception.GeneralException;
import cz.inqool.arclib.store.JobStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static cz.inqool.arclib.util.Utils.notNull;

@Slf4j
@Service
public class JobRunner {
    private Map<String, ScheduledFuture> jobIdToScheduleFuture = new HashMap<>();
    private ThreadPoolTaskScheduler scheduler;
    private JobStore jobStore;

    private void run(Job job) {
        notNull(job, () -> new BadArgument("job"));

        switch (job.getScriptType()) {
            default:
            case SHELL:
                runShell(job.getScript());
                break;
        }
    }

    /**
     * Schedules the job.
     * @param job job to schedule
     */
    public void schedule(Job job) {
        CronTrigger trigger = new CronTrigger(job.getTiming());

        scheduler = scheduler();
        ScheduledFuture<?> future = scheduler.schedule(() -> run(job), trigger);
        jobIdToScheduleFuture.put(job.getId(), future);
    }

    /**
     * Cancels scheduling of the job.
     * @param job to unschedule
     */
    public void unschedule(Job job) {
        notNull(job, () -> new BadArgument("job"));

        ScheduledFuture scheduledFuture = jobIdToScheduleFuture.get(job.getId());
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            jobIdToScheduleFuture.remove(job.getId());

            job.setActive(false);
            jobStore.save(job);
        }
    }

    private void runShell(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder(script);

            pb.start();
        } catch (IOException e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Returns instance of the scheduler.
     * @return instance of the scheduler
     */
    public ThreadPoolTaskScheduler scheduler() {
        if (scheduler == null) {
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(10);
            scheduler.afterPropertiesSet();
        }
        return scheduler;
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }
}
