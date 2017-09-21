package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.Job;
import cz.cas.lib.arclib.helper.ApiTest;
import cz.cas.lib.arclib.store.JobStore;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.helper.ThrowableAssertion;
import liquibase.util.SystemUtils;
import org.junit.BeforeClass;
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

    private static String SCRIPT_1;
    private static String SCRIPT_2;
    private static String SCRIPT_3;

    @BeforeClass
    public static void setScriptFiles() {
        if (SystemUtils.IS_OS_WINDOWS) {
            SCRIPT_1 = "/script1.bat";
            SCRIPT_2 = "/script2.bat";
            SCRIPT_3 = "/script3.bat";
        } else {
            SCRIPT_1 = "/script1.sh";
            SCRIPT_2 = "/script2.sh";
            SCRIPT_3 = "/script3.sh";
        }
    }

    @Test
    public void scheduleTestScriptReturnsZero() throws InterruptedException {
        Instant start = Instant.now();

        Job job = new Job();
        job.setScriptPath((this.getClass().getResource(SCRIPT_1)).getPath());
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
        job.setScriptPath((this.getClass().getResource(SCRIPT_2)).getPath());

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
        job.setScriptPath((this.getClass().getResource(SCRIPT_3)).getPath());
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
    public void scheduleTestMultipleParallelScripts() throws InterruptedException {
        Instant start = Instant.now();

        Job job1 = new Job();
        job1.setScriptPath((this.getClass().getResource(SCRIPT_1)).getPath());
        job1.setTiming("*/1 * * * * ?");
        store.save(job1);

        String job1Id = job1.getId();

        api.schedule(job1Id);

        Job job2 = new Job();
        job2.setScriptPath((this.getClass().getResource(SCRIPT_2)).getPath());
        job2.setTiming("*/1 * * * * ?");
        store.save(job2);

        String job2Id = job2.getId();

        api.schedule(job2Id);

        Job job3 = new Job();
        job3.setScriptPath((this.getClass().getResource(SCRIPT_3)).getPath());
        job3.setTiming("*/1 * * * * ?");
        store.save(job3);

        String job3Id = job3.getId();

        api.schedule(job3Id);

        Thread.sleep(2000);

        job1 = store.find(job1.getId());
        assertThat(job1.getActive(), is(true));
        assertThat(job1.getLastReturnCode(), is(0));
        assertThat(job1.getLastExecutionTime().isAfter(start), is(true));
        assertThat(job1.getLastExecutionTime().isBefore(Instant.now()), is(true));

        job2 = store.find(job2.getId());
        assertThat(job2.getActive(), is(true));
        assertThat(job2.getLastReturnCode(), is(1));
        assertThat(job2.getLastExecutionTime().isAfter(start), is(true));
        assertThat(job2.getLastExecutionTime().isBefore(Instant.now()), is(true));

        job3 = store.find(job3.getId());
        assertThat(job3.getActive(), is(true));
        assertThat(job3.getLastReturnCode(), is(-1));
        assertThat(job3.getLastExecutionTime().isAfter(start), is(true));
        assertThat(job3.getLastExecutionTime().isBefore(Instant.now()), is(true));
    }

    @Test
    public void unscheduleTest() throws InterruptedException {
        Instant start = Instant.now();

        Job job = new Job();
        job.setScriptPath((this.getClass().getResource(SCRIPT_1)).getPath());
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
        assertThat(job.getLastExecutionTime().isBefore(finish), is(true));
    }

    @Test
    public void unscheduleNonExistentJobTest() throws InterruptedException {
        ThrowableAssertion.assertThrown(() -> api.unschedule("%#%@#")).isInstanceOf(BadArgument.class);
    }
}
