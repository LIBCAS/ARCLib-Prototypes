package cas.lib.arclib;

import cas.lib.arclib.domain.Job;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.GeneralException;
import cas.lib.arclib.store.JobStore;
import cz.inqool.uas.util.Utils;
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

        if (exitCode == 0) {
            log.info("Script execution of job " + job.getId() + " has finished successfully.");
        } else {
            log.error("Script execution of job " + job.getId() + " has failed.");
        }

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
            switch (p.waitFor()) {
                case 0:
                    return 0;
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
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }
}
