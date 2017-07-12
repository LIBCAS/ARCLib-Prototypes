package cz.inqool.arclib.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.BatchState;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.domain.SipState;
import cz.inqool.arclib.exception.ForbiddenObject;
import cz.inqool.arclib.exception.MissingObject;
import cz.inqool.arclib.helper.DbTest;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static cz.inqool.arclib.helper.ThrowableAssertion.assertThrown;
import static cz.inqool.arclib.util.Utils.asSet;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerServiceTest extends DbTest {
    private BatchStore batchStore;
    private WorkerService service;
    private SipStore sipStore;

    @Autowired
    private JmsTemplate template;

    @Autowired
    private RuntimeService runtimeService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        batchStore = new BatchStore();
        batchStore.setEntityManager(getEm());
        batchStore.setQueryFactory(new JPAQueryFactory(getEm()));

        sipStore = new SipStore();
        sipStore.setEntityManager(getEm());
        sipStore.setQueryFactory(new JPAQueryFactory(getEm()));

        service = new WorkerService();
        service.setBatchStore(batchStore);
        service.setRuntimeService(runtimeService);
        service.setSipStore(sipStore);
        service.setTemplate(template);
    }

    @Test
    public void stopAtMultipleFailuresTestHalfPackagesFailed() throws InterruptedException {
        Sip sip1 = new Sip();
        sip1.setState(SipState.PROCESSING);
        sipStore.save(sip1);

        Sip sip2 = new Sip();
        sip2.setState(SipState.FAILED);
        sipStore.save(sip2);

        flushCache();

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIds(asSet(sip1.getId(), sip2.getId()));
        batchStore.save(batch);

        service.processSip(new CoordinatorDto(sip2.getId(), batch.getId()));

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.PROCESSING));
    }

    @Ignore
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

        flushCache();

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIds(asSet(sip4.getId(), sip5.getId(), sip6.getId()));
        batchStore.save(batch);

        service.processSip(new CoordinatorDto(sip6.getId(), batch.getId()));

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.CANCELED));
    }

    @Test
    public void processSipTestNonExistentSip() {
        String sipId = "%@#@^#$#";

        Batch batch = new Batch();
        batch.setIds(asSet(sipId));
        batchStore.save(batch);

        assertThrown(() -> service.processSip(new CoordinatorDto(sipId, batch.getId()))).isInstanceOf(MissingObject.class);
    }

    @Test
    public void processSipTestNonExistentBatch() throws InterruptedException {
        Sip sip1 = new Sip();
        sip1.setState(SipState.PROCESSING);
        sipStore.save(sip1);

        assertThrown(() -> service.processSip(new CoordinatorDto(sip1.getId(), "%@#@^#$#"))).isInstanceOf(MissingObject.class);
    }

    @Test
    public void processSipTestInvalidSip() {
        Sip sip3 = new Sip();
        sip3.setState(SipState.PROCESSING);
        sipStore.save(sip3);

        Batch batch3 = new Batch();
        batchStore.save(batch3);

        assertThrown(() -> service.processSip(new CoordinatorDto(sip3.getId(), batch3.getId()))).isInstanceOf(ForbiddenObject.class);
    }
}
