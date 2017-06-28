package cz.inqool.arclib;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import cz.inqool.arclib.store.JobStore;
import cz.inqool.arclib.domain.Job;

import javax.inject.Inject;
import java.util.List;

/**
 * Gathers timers from Database and add triggers for every timer to spring.
 */
@Slf4j
@Configuration
public class SchedulingConfiguration implements SchedulingConfigurer {
    private JobStore jobStore;

    private JobRunner runner;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        List<Job> jobs = jobStore.findAllActive();

        log.info("Starting timers...");

        jobs.forEach(job -> {
            log.info("{} set to: {}", job.getName());
            taskRegistrar.addTriggerTask(() -> runner.run(job), new CronTrigger(job.getTiming()));
        });
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }

    @Inject
    public void setRunner(JobRunner runner) {
        this.runner = runner;
    }
}
