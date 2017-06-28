package cz.inqool.arclib;

import cz.inqool.arclib.domain.Job;
import cz.inqool.arclib.exception.BadArgument;
import cz.inqool.arclib.exception.GeneralException;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static cz.inqool.arclib.util.Utils.notNull;

@Service
public class JobRunner {
    public void run(Job job) {
        notNull(job, () -> new BadArgument("job"));

        switch (job.getScriptType()) {
            default:
            case SHELL:
                runShell(job.getScript());
                break;
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
}
