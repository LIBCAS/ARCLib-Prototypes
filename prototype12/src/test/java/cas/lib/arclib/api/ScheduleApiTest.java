package cas.lib.arclib.api;

import cas.lib.arclib.domain.Job;
import cas.lib.arclib.helper.ApiTest;
import cas.lib.arclib.store.JobStore;
import cz.inqool.uas.exception.BadArgument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import static cas.lib.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
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
        assertThrown(() -> api.schedule("%#%@#")).isInstanceOf(BadArgument.class);
    }

    @Test
    public void scheduleTest() throws InterruptedException {
        Job job = new Job();
        job.setScript("%#%@#");
        job.setTiming("*/1 * * * * ?");
        store.save(job);

        String jobId = job.getId();

        api.schedule(jobId);

        Thread.sleep(2000);

        job = store.find(job.getId());
        assertThat(job.getActive(), is(true));
    }

    @Test
    public void unscheduleNonExistentJobTest() throws InterruptedException {
        assertThrown(() -> api.unschedule("%#%@#")).isInstanceOf(BadArgument.class);
    }

    @Test
    public void unscheduleTest() throws InterruptedException {
        Job job = new Job();
        job.setActive(true);
        store.save(job);

        api.unschedule(job.getId());

        Thread.sleep(2000);

        job = store.find(job.getId());
        assertThat(job.getActive(), is(false));
    }
}
