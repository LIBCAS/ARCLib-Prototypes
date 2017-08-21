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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
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

    /**
     * Schedules the job.
     *
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
     *
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

    /**
     * Returns instance of the scheduler.
     *
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

    private void run(Job job) {
        notNull(job, () -> new BadArgument("job"));

        int exitCode = runShell(job.getScript());
        job.setLastReturnCode(exitCode);
        job.setLastExecutionTime(Instant.now());
        jobStore.save(job);
    }

    public int runShell(String script) {
        notNull(script, () -> {
            throw new IllegalArgumentException("cannot run provided script, the script is null");
        });
        ProcessBuilder pb = new ProcessBuilder(script, "-r");

        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new GeneralException(e.toString());
        }

        final int[] value = new int[1];
        Thread commandLineThread = new Thread(() -> {
            try {
                switch (p.waitFor()) {
                    case 0:
                        log.info("script has finished successfully");
                        value[0] = 0;
                    default:
                        BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        StringBuilder sb = new StringBuilder();
                        String line = br.readLine();
                        while (line != null) {
                            sb.append(line);
                            line = br.readLine();
                        }
                        throw new GeneralException(sb.toString());
                }
            } catch (InterruptedException e) {
                throw new GeneralException(e);
            } catch (IOException e) {
                throw new GeneralException(e);
            }
        });
        commandLineThread.setDaemon(true);
        commandLineThread.start();

        return value[0];
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }
}
