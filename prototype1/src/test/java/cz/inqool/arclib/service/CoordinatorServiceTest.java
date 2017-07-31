package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.*;
import cz.inqool.arclib.exception.GeneralException;
import cz.inqool.arclib.exception.MissingObject;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.arclib.helper.ThrowableAssertion.assertThrown;
import static cz.inqool.arclib.util.Utils.asSet;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CoordinatorServiceTest {
    @Inject
    private CoordinatorService service;

    @Inject
    private BatchStore batchStore;

    @Inject
    private SipStore sipStore;

    @Inject
    private JmsTemplate template;

    /**
     * Test of ({@link CoordinatorService#start(String)}) method. The test asserts that a GeneralException is thrown when a non existent path
     * is provided.
     */
    @Test
    public void startTestNonExistentPath() {
        assertThrown(() -> service.start("/nonExistentPath"))
                .isInstanceOf(GeneralException.class);
    }

    /**
     * Test of ({@link CoordinatorService#start(String)}) method. The method is passed a path to the test folder containing three empty
     * files. The test asserts that:
     *
     * 1. there is a new instance of batch created and its state is PROCESSING
     * 2. there are three SIP packages created (one for each file) and their state is PROCESSED
     * 3. SIP ids that belong to the batch are the same as the ids of the SIP packages stored in database
     */
    @Test
    public void startTest() throws InterruptedException {
        String batchId = service.start(
                getClass().getResource("/testFiles").getPath());

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(6000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.PROCESSING));

        Collection<Sip> allSips = sipStore.findAll();
        assertThat(allSips, hasSize(3));
        allSips.forEach(sip -> {
            assertThat(sip.getState(), is(SipState.PROCESSED));
        });

        List<String> allSipsIds = allSips.stream()
                .map(DomainObject::getId)
                .collect(Collectors.toList());
        Set<String> batchSipIds = batch.getIds();
        assertThat(batchSipIds.toArray(), is(allSipsIds.toArray()));
    }

    /**
     * Test of ({@link CoordinatorService#cancel(String)}) method. There are two methods called in a row:
     * 1. method ({@link CoordinatorService#start(String)}) passed a path to the test folder containing three empty files
     * 2. method ({@link CoordinatorService#cancel(String)}) that cancels the batch
     *
     * The test asserts that:
     * 1. the state of the batch is CANCELED
     * 2. there is at least one unprocessed SIP
     */
    @Test
    public void cancelTest() throws InterruptedException {
        String batchId = service.start(
                getClass().getResource("/testFiles").getPath());

        service.cancel(batchId);

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(6000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.CANCELED));

        Collection<Sip> allSips = sipStore.findAll();
        boolean hasUnprocessedSip = allSips.stream()
                .anyMatch(sip -> sip.getState() != SipState.PROCESSED);

        assertThat(hasUnprocessedSip, is(true));
    }

    /**
     * Test of ({@link CoordinatorService#suspend(String)}) method. It will assert that a MissingObject exception is thrown
     * when ID of a non existent batch is provided.
     */
    @Test
    public void suspendNonExistentBatchTest() {
        assertThrown(() -> service.suspend("#%#$")).isInstanceOf(MissingObject.class);
    }

    /**
     * Test of ({@link CoordinatorService#cancel(String)}) method. There are two methods called in a row:
     * 1. method ({@link CoordinatorService#start(String)}) passed a path to the test folder containing three empty files
     * 2. method ({@link CoordinatorService#suspend(String)}) that suspends the batch
     *
     * 1. the state of the batch is SUSPENDED
     * 2. there is at least one unprocessed SIP
     */
    @Test
    public void suspendTest() throws InterruptedException {
        String batchId = service.start(
                getClass().getResource("/testFiles").getPath());

        service.suspend(batchId);

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(6000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.SUSPENDED));

        Collection<Sip> allSips = sipStore.findAll();
        boolean hasUnprocessedSip = allSips.stream()
                .anyMatch(sip -> sip.getState() == SipState.NEW);

        assertThat(hasUnprocessedSip, is(true));
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. It will assert that a MissingObject exception is thrown
     * when ID of a non existent batch is provided.
     */
    @Test
    public void resumeTestNonExistentBatch() {
        assertThrown(() -> service.resume("#%#$")).isInstanceOf(MissingObject.class);
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. The method is passed ID of a batch that:
     * 1. is in the state SUSPENDED
     * 2. has a SIP package in the state PROCESSING
     *
     * The test asserts that:
     * 1. the batch has not resumed (the return value of the method is false)
     * 2. the batch state is SUSPENDED
     */
    @Test
    public void resumeTestSipWithStateProcessing() throws InterruptedException {
        Sip sip = new Sip();
        sip.setState(SipState.PROCESSING);
        sipStore.save(sip);

        Batch batch = new Batch();
        batch.setState(BatchState.SUSPENDED);
        batch.setIds(asSet(sip.getId()));
        batchStore.save(batch);

        Boolean hasResumed = service.resume(batch.getId());

        Thread.sleep(2000);

        assertThat(hasResumed, is(false));
        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.SUSPENDED));
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. The method is passed ID of a batch that:
     * 1. is in the state SUSPENDED
     * 2. has no SIP packages in the state PROCESSING, has only a SIP package in the state NEW
     *
     * The test asserts that:
     * 1. the batch has successfuly resumed (the return value of the method is true)
     * 2. the batch state is PROCESSING
     */
    @Test
    public void resumeTestNoSipWithStateProcessing() throws InterruptedException {
        Sip sip = new Sip();
        sip.setState(SipState.NEW);
        sipStore.save(sip);

        Batch batch = new Batch();
        batch.setState(BatchState.SUSPENDED);
        batch.setIds(asSet(sip.getId()));
        batchStore.save(batch);

        Boolean hasResumed2 = service.resume(batch.getId());

        Thread.sleep(2000);

        assertThat(hasResumed2, is(true));
        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.PROCESSING));

        sip = sipStore.find(sip.getId());
        assertThat(sip.getState(), is(SipState.PROCESSED));
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. There are three methods called in a sequence:
     * 1. method ({@link CoordinatorService#start(String}) passed a path to the test folder containing three empty files
     * 2. method ({@link CoordinatorService#suspend(String}) that suspends the batch and than waits for 2 seconds
     * 3. method ({@link CoordinatorService#resume(String}) that resumes the batch
     *
     * The test asserts that:
     * 1. the batch is in the state PROCESSING
     * 2. there are three SIP packages created (one for each file) and their state is PROCESSED
     * 3. sip ids that belong to the batch are the same as the ids of the sip packages stored in database
     */
    @Test
    public void resumeTest() throws InterruptedException {
        String batchId = service.start(
                getClass().getResource("/testFiles").getPath());

        service.suspend(batchId);
        Thread.sleep(4000);

        service.resume(batchId);
        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(20000);

        Batch batch = batchStore.find(batchId);
        assertThat(batch.getState(), is(BatchState.PROCESSING));

        Collection<Sip> allSips = sipStore.findAll();
        assertThat(allSips, hasSize(3));
        allSips.forEach(sip -> {
            assertThat(sip.getState(), is(SipState.PROCESSED));
        });

        List<String> allSipsIds = allSips.stream()
                .map(DomainObject::getId)
                .collect(Collectors.toList());
        Set<String> batchSipIds = batch.getIds();
        assertThat(batchSipIds.toArray(), is(allSipsIds.toArray()));
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
