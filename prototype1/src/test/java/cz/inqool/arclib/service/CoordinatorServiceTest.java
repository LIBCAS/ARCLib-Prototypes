package cz.inqool.arclib.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.BatchState;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.exception.GeneralException;
import cz.inqool.arclib.exception.MissingObject;
import cz.inqool.arclib.helper.DbTest;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.core.JmsTemplate;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import static cz.inqool.arclib.helper.ThrowableAssertion.assertThrown;
import static cz.inqool.arclib.util.Utils.asSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CoordinatorServiceTest extends DbTest {
    private CoordinatorService service;
    private BatchStore batchStore;
    private SipStore sipStore;
    private JmsTemplate template = Mockito.mock(JmsTemplate.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        batchStore = new BatchStore();
        batchStore.setEntityManager(getEm());
        batchStore.setQueryFactory(new JPAQueryFactory(getEm()));

        sipStore = new SipStore();
        sipStore.setEntityManager(getEm());
        sipStore.setQueryFactory(new JPAQueryFactory(getEm()));

        service = new CoordinatorService();
        service.setSipStore(sipStore);
        service.setBatchStore(batchStore);
        service.setTemplate(template);
    }

    @Test
    public void runTest() {
        assertThrown(() -> service.run("/nonExistentPath"))
                .isInstanceOf(GeneralException.class);

        service.run(getClass().getResource("/testFiles").getPath());

        Collection<Sip> allSips = sipStore.findAll();
        assertThat(allSips, hasSize(3));

        Iterator<Sip> iterator = allSips.iterator();
        Sip sip1 = iterator.next();
        Sip sip2 = iterator.next();
        Sip sip3 = iterator.next();

        Collection<Batch> allBatches = batchStore.findAll();
        assertThat(allBatches, hasSize(1));

        Batch batch = allBatches.iterator().next();
        Set<String> ids = batch.getIds();

        assertThat(ids, containsInAnyOrder(sip1.getId(), sip2.getId(), sip3.getId()));
        assertThat(batch.getState(), is(BatchState.PROCESSING));
    }

    @Test
    public void cancelTest() {
        assertThrown(() -> service.cancel("#%#$")).isInstanceOf(MissingObject.class);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batchStore.save(batch);

        flushCache();

        service.cancel(batch.getId());

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.CANCELED));
    }

    @Test
    public void suspendTest() {
        assertThrown(() -> service.suspend("#%#$")).isInstanceOf(MissingObject.class);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batchStore.save(batch);

        flushCache();

        service.suspend(batch.getId());

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.SUSPENDED));
    }

    @Test
    public void resumeTest() {
        assertThrown(() -> service.resume("#%#$")).isInstanceOf(MissingObject.class);

        /*
        No SIPs
         */
        Batch batch1 = new Batch();
        batch1.setState(BatchState.SUSPENDED);
        batchStore.save(batch1);

        flushCache();

        service.resume(batch1.getId());

        batch1 = batchStore.find(batch1.getId());
        assertThat(batch1.getState(), is(BatchState.PROCESSING));

        /*
        Batch has SIP with state PROCESSING
        */
        Sip sip1 = new Sip();
        sip1.setState(SipState.PROCESSING);
        sipStore.save(sip1);

        Batch batch2 = new Batch();
        batch2.setState(BatchState.SUSPENDED);
        batch2.setIds(asSet(sip1.getId()));
        batchStore.save(batch2);

        flushCache();

        Boolean hasResumed = service.resume(batch2.getId());

        assertThat(hasResumed, is(false));
        batch2 = batchStore.find(batch2.getId());
        assertThat(batch2.getState(), is(BatchState.SUSPENDED));

        /*
        Batch has no SIPs with state PROCESSING
        */
        Sip sip2 = new Sip();
        sip2.setState(SipState.NEW);
        sipStore.save(sip2);

        Batch batch3 = new Batch();
        batch3.setState(BatchState.SUSPENDED);
        batch2.setIds(asSet(sip2.getId()));
        batchStore.save(batch3);

        flushCache();

        Boolean hasResumed2 = service.resume(batch3.getId());

        assertThat(hasResumed2, is(true));
        batch3 = batchStore.find(batch3.getId());
        assertThat(batch3.getState(), is(BatchState.PROCESSING));
    }
}
