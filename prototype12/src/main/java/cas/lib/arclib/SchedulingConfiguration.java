package cas.lib.arclib;

import cas.lib.arclib.domain.Job;
import cas.lib.arclib.store.JobStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.inject.Inject;
import java.util.List;

/**
 * Gathers timers from Database and add triggers for every timer to spring.
 */
@Slf4j
@Configuration
    public class SchedulingConfiguration implements SchedulingConfigurer {
    private JobStore jobStore;

    private JobScheduler jobScheduler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(jobScheduler.scheduler());

        List<Job> jobs = jobStore.findAllActive();

        log.info("Starting timers...");

        jobs.forEach(job -> {
            jobScheduler.schedule(job);
        });
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }

    @Inject
    public void setJobScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }
}
