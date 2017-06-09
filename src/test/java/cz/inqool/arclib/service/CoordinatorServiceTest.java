package cz.inqool.arclib.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.arclib.domain.Batch;
import cz.inqool.arclib.domain.Sip;
import cz.inqool.arclib.helper.DbTest;
import cz.inqool.arclib.store.BatchStore;
import cz.inqool.arclib.store.SipStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.core.JmsTemplate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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
    }
}
