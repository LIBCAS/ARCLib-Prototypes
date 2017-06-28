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
     * Runs the job now.
     * @param id Id of the {@link Job}
     */
    @Transactional
    @ApiOperation(value = "Runs the job now.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Job not found")})
    @RequestMapping(value = "/{id}/run", method = RequestMethod.POST)
    public void run(@ApiParam(value = "Id of the sequence", required = true)
                    @PathVariable("id") String id) {
        Job job = adapter.find(id);
        runner.run(job);
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
