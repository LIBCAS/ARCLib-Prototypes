package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.Job;
import cz.cas.lib.arclib.helper.ApiTest;
import cz.cas.lib.arclib.store.JobStore;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.helper.ThrowableAssertion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import java.time.Instant;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ScheduleApiTest implements ApiTest {
    @Inject
    private ScheduleApi api;

    @Inject
    private JobStore store;

    @Test
    public void scheduleNonExistentJobTest() throws InterruptedException {
        ThrowableAssertion.assertThrown(() -> api.schedule("%#%@#")).isInstanceOf(BadArgument.class);
    }

    @Test
    public void scheduleTestScriptReturnsZero() throws InterruptedException {
        Instant start = Instant.now();

        Job job = new Job();
        job.setScript((this.getClass().getResource("/script1.bat")).getPath());
        job.setTiming("*/1 * * * * ?");
        store.save(job);

        String jobId = job.getId();

        api.schedule(jobId);

        Thread.sleep(2000);

        job = store.find(job.getId());
        assertThat(job.getActive(), is(true));
        assertThat(job.getLastReturnCode(), is(0));
        assertThat(job.getLastExecutionTime().isAfter(start), is(true));
        assertThat(job.getLastExecutionTime().isBefore(Instant.now()), is(true));
    }

    @Test
    public void scheduleTestScriptReturnsOne() throws InterruptedException {
        Instant start = Instant.now();

        Job job = new Job();
        job.setScript((this.getClass().getResource("/script2.bat")).getPath());
        job.setTiming("*/1 * * * * ?");
        store.save(job);

        String jobId = job.getId();

        api.schedule(jobId);

        Thread.sleep(2000);

        job = store.find(job.getId());
        assertThat(job.getActive(), is(true));
        assertThat(job.getLastReturnCode(), is(1));
        assertThat(job.getLastExecutionTime().isAfter(start), is(true));
        assertThat(job.getLastExecutionTime().isBefore(Instant.now()), is(true));
    }

    @Test
    public void scheduleTestScriptReturnsMinusOne() throws InterruptedException {
        Instant start = Instant.now();

        Job job = new Job();
        job.setScript((this.getClass().getResource("/script3.bat")).getPath());
        job.setTiming("*/1 * * * * ?");
        store.save(job);

        String jobId = job.getId();

        api.schedule(jobId);

        Thread.sleep(2000);

        job = store.find(job.getId());
        assertThat(job.getActive(), is(true));
        assertThat(job.getLastReturnCode(), is(-1));
        assertThat(job.getLastExecutionTime().isAfter(start), is(true));
        assertThat(job.getLastExecutionTime().isBefore(Instant.now()), is(true));
    }

    @Test
    public void unscheduleNonExistentJobTest() throws InterruptedException {
        ThrowableAssertion.assertThrown(() -> api.unschedule("%#%@#")).isInstanceOf(BadArgument.class);
    }

    @Test
    public void unscheduleTest() throws InterruptedException {
        Instant start = Instant.now();

        Job job = new Job();
        job.setScript((this.getClass().getResource("/script1.bat")).getPath());
        job.setTiming("*/1 * * * * ?");
        store.save(job);

        String jobId = job.getId();

        api.schedule(jobId);

        Thread.sleep(2000);

        api.unschedule(job.getId());

        Thread.sleep(2000);

        Instant finish = Instant.now();

        Thread.sleep(2000);

        job = store.find(job.getId());
        assertThat(job.getActive(), is(false));
        assertThat(job.getLastExecutionTime().isAfter(start), is(true));
        assertThat(job.getLastExecutionTime().isBefore(finish), is(true));    }
}
