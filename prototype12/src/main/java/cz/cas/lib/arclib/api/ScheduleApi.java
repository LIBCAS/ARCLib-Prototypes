package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.JobScheduler;
import cz.cas.lib.arclib.domain.Job;
import cz.cas.lib.arclib.store.JobStore;
import cz.cas.lib.arclib.store.general.Transactional;
import lombok.Getter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleApi {
    private JobScheduler scheduler;

    @Getter
    private JobStore adapter;

    /**
     * Schedules the job.
     *
     * @param id Id of the {@link Job}
     */
    @Transactional
    @RequestMapping(value = "/{id}/schedule", method = RequestMethod.POST)
    public void schedule(@PathVariable("id") String id) {
        Job job = adapter.find(id);
        scheduler.schedule(job);
    }

    /**
     * Unschedules the job.
     * |
     *
     * @param id Id of the {@link Job}
     */
    @Transactional
    @RequestMapping(value = "/{id}/unschedule", method = RequestMethod.POST)
    public void unschedule(@PathVariable("id") String id) {
        Job job = adapter.find(id);
        scheduler.unschedule(job);
    }

    @Inject
    public void setAdapter(JobStore store) {
        this.adapter = store;
    }

    @Inject
    public void setScheduler(JobScheduler scheduler) {
        this.scheduler = scheduler;
    }
}
