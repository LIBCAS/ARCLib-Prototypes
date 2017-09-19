package cz.cas.lib.arclib;

import cz.cas.lib.arclib.domain.Job;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.exception.GeneralException;
import cz.cas.lib.arclib.store.JobStore;
import cz.cas.lib.arclib.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;

@Slf4j
@Service
public class JobRunner {
    private JobStore jobStore;

    /**
     * Run the job and store the return value and time of the execution to database.
     *
     * @param job job to run
     */
    public void run(Job job) {
        Utils.notNull(job, () -> new BadArgument("job"));

        log.info("Starting script of job " + job.getId() + ".");

        int exitCode = runShell(job.getScript());

        log.info("Script execution of job " + job.getId() + " has finished with code " + exitCode + ".");

        job.setLastReturnCode(exitCode);
        job.setLastExecutionTime(Instant.now());

        jobStore.save(job);
    }

    /**
     * Run the shell script and return its return code
     * @param script to run
     * @return return code of the script execution
     */
    private int runShell(String script) {
        Utils.notNull(script, () -> {
            throw new IllegalArgumentException("cannot run provided script, the script is null");
        });
        ProcessBuilder pb = new ProcessBuilder(script);

        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new GeneralException(e.toString());
        }

        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            throw new GeneralException(e);
        }
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }
}
