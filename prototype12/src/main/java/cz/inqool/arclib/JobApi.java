package cz.inqool.arclib;

import cz.inqool.arclib.rest.GeneralApi;
import cz.inqool.arclib.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import cz.inqool.arclib.domain.Job;
import cz.inqool.arclib.store.JobStore;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

/**
 * Api for managing jobs.
 */
@RolesAllowed("ROLE_JOB")
@RestController
@Api(value = "job", description = "Api for managing jobs")
@RequestMapping("/api/jobs")
public class JobApi implements GeneralApi<Job> {
    private JobRunner runner;

    @Getter
    private JobStore adapter;

    /**
     * Schedules the job.
     * @param id Id of the {@link Job}
     */
    @Transactional
    @ApiOperation(value = "Schedules the job now.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Job not found")})
    @RequestMapping(value = "/{id}/schedule", method = RequestMethod.POST)
    public void schedule(@ApiParam(value = "Id of the sequence", required = true)
                    @PathVariable("id") String id) {
        Job job = adapter.find(id);
        runner.schedule(job);
    }

    /**
     * Unschedules the job.
     * @param id Id of the {@link Job}
     */
    @Transactional
    @ApiOperation(value = "Unschedules the job.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Job not found")})
    @RequestMapping(value = "/{id}/unschedule", method = RequestMethod.POST)
    public void unschedule(@ApiParam(value = "Id of the sequence", required = true)
                    @PathVariable("id") String id) {
        Job job = adapter.find(id);
        runner.unschedule(job);
    }

    @Inject
    public void setAdapter(JobStore store) {
        this.adapter = store;
    }

    @Inject
    public void setRunner(JobRunner runner) {
        this.runner = runner;
    }
}
