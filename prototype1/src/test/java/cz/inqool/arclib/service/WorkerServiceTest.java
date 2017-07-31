package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.BatchState;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.sql.SQLException;

import static cz.inqool.arclib.util.Utils.asSet;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerServiceTest {

    @Inject
    private BatchStore batchStore;

    @Inject
    private WorkerService service;

    @Inject
    private SipStore sipStore;

    @Inject
    private JmsTemplate template;

    @Inject
    private RuntimeService runtimeService;

    /**
     * Test of ({@link WorkerService#processSip(CoordinatorDto)}) method. First, there is a batch created that:
     * 1. is in the state PROCESSING
     * 2. has one SIP package in state NEW
     * Then the processSip method is called on the given SIP.
     * <p>
     * The test asserts that in the end the SIP package is in the state PROCESSED.
     */
    @Test
    public void processSipTest() throws InterruptedException {
        Sip sip = new Sip();
        sip.setState(SipState.NEW);
        sipStore.save(sip);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIds(asSet(sip.getId()));
        batchStore.save(batch);

        service.processSip(new CoordinatorDto(sip.getId(), batch.getId()));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(6000);

        sip = sipStore.find(sip.getId());

        assertThat(sip.getState(), is(SipState.PROCESSED));
    }

    /**
     * Test of ({@link WorkerService#processSip(CoordinatorDto)}) method. First, there is a batch created that:
     * 1. is in the state SUSPENDED
     * 2. has one SIP package in state NEW
     * Then the processSip method is called on the given SIP.
     * <p>
     * The test asserts that in the end the SIP package is still in the state NEW. The SIP remained unprocessed because the respective batch
     * was in the state SUSPENDED.
     */
    @Test
    public void processSipTestBatchStateSuspended() throws InterruptedException {
        Sip sip1 = new Sip();
        sip1.setState(SipState.NEW);
        sipStore.save(sip1);

        Batch batch = new Batch();
        batch.setState(BatchState.SUSPENDED);
        batch.setIds(asSet(sip1.getId()));
        batchStore.save(batch);

        service.processSip(new CoordinatorDto(sip1.getId(), batch.getId()));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(6000);

        sip1 = sipStore.find(sip1.getId());

        assertThat(sip1.getState(), is(SipState.NEW));
    }

    /**
     * Test of ({@link WorkerService#processSip(CoordinatorDto)}) method. First, there is a batch created that:
     * 1. is in the state CANCELED
     * 2. has one SIP package in state NEW
     * Then the processSip method is called on the given SIP.
     * <p>
     * The test asserts that in the end the SIP package is still in the state NEW. The SIP remained unprocessed because the respective batch
     * was in the state CANCELED.
     */
    @Test
    public void processSipTestBatchStateCanceled() throws InterruptedException {
        Sip sip1 = new Sip();
        sip1.setState(SipState.NEW);
        sipStore.save(sip1);

        Batch batch = new Batch();
        batch.setState(BatchState.CANCELED);
        batch.setIds(asSet(sip1.getId()));
        batchStore.save(batch);

        service.processSip(new CoordinatorDto(sip1.getId(), batch.getId()));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(6000);

        sip1 = sipStore.find(sip1.getId());

        assertThat(sip1.getState(), is(SipState.NEW));
    }

    /**
     * Test of ({@link WorkerService#processSip(CoordinatorDto)} method. The test asserts that on the processing of SIP package the
     * respective batch is not canceled when only half of the batch SIP packages have state FAILED.
     */
    @Test
    public void stopAtMultipleFailuresTestHalfPackagesFailed() throws InterruptedException {
        Sip sip1 = new Sip();
        sip1.setState(SipState.FAILED);
        sipStore.save(sip1);

        Sip sip2 = new Sip();
        sip2.setState(SipState.NEW);
        sipStore.save(sip2);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIds(asSet(sip1.getId(), sip2.getId()));
        batchStore.save(batch);

        service.processSip(new CoordinatorDto(sip2.getId(), batch.getId()));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(2000);

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.PROCESSING));
    }

    /**
     * Test of ({@link WorkerService#processSip(CoordinatorDto)} method. The test asserts that on the processing of SIP package the
     * respective batch is canceled when more than half of the batch SIP packages have state FAILED.
     */
    @Test
    public void stopAtMultipleFailuresTestMoreThanHalfPackagesFailed() throws InterruptedException {
        Sip sip4 = new Sip();
        sip4.setState(SipState.PROCESSING);
        sipStore.save(sip4);

        Sip sip5 = new Sip();
        sip5.setState(SipState.FAILED);
        sipStore.save(sip5);

        Sip sip6 = new Sip();
        sip6.setState(SipState.FAILED);
        sipStore.save(sip6);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIds(asSet(sip4.getId(), sip5.getId(), sip6.getId()));
        batchStore.save(batch);

        service.processSip(new CoordinatorDto(sip6.getId(), batch.getId()));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(2000);

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.CANCELED));
    }

    @After
    public void testTearDown() throws SQLException {
        sipStore.findAll().forEach(sip -> {
            sipStore.delete(sip);
        });

        batchStore.findAll().forEach(batch -> {
            batchStore.delete(batch);
        });
    }
}
