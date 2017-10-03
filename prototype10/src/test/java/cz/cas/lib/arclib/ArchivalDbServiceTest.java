package cz.cas.lib.arclib;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.XmlState;
import cz.cas.lib.arclib.exception.*;
import cz.cas.lib.arclib.helper.DbTest;
import cz.cas.lib.arclib.service.ArchivalDbService;
import cz.cas.lib.arclib.store.AipSipStore;
import cz.cas.lib.arclib.store.AipXmlStore;
import cz.cas.lib.arclib.store.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ArchivalDbServiceTest extends DbTest {

    @Rule
    public TestName name = new TestName();

    private static final AipXmlStore xmlStore = new AipXmlStore();
    private static final AipSipStore sipStore = new AipSipStore();
    private static final ArchivalDbService service = new ArchivalDbService();

    private static final String SIP_ID = "SIPtestID";
    private static final String XML1_ID = "XML1testID";
    private static final String XML2_ID = "XML2testID";
    private static final String S = "somestring";

    @Before
    public void before() {
        xmlStore.setEntityManager(getEm());
        xmlStore.setQueryFactory(new JPAQueryFactory(getEm()));

        sipStore.setEntityManager(getEm());
        sipStore.setQueryFactory(new JPAQueryFactory(getEm()));

        service.setAipSipStore(sipStore);
        service.setAipXmlStore(xmlStore);

        AipSip sip = new AipSip(SIP_ID, S, AipState.ARCHIVED);
        sipStore.save(sip);
        xmlStore.save(new AipXml(XML1_ID, S, sip, 1, XmlState.ARCHIVED));
        xmlStore.save(new AipXml(XML2_ID, S, sip, 2, XmlState.ARCHIVED));
    }

    @After
    public void after() throws SQLException {
        clearDatabase();
    }

    @Test
    public void registerAipCreation() {
        String xmlId = service.registerAipCreation(name.getMethodName(), S, S);
        assertThat(sipStore.find(name.getMethodName()).getState(), equalTo(AipState.PROCESSING));
        assertThat(xmlStore.find(xmlId).getState(), equalTo(XmlState.PROCESSING));
    }

    @Test
    public void finishAipCreation() {
        String xmlId = service.registerAipCreation(name.getMethodName(), S, S);
        service.finishAipCreation(name.getMethodName(), xmlId);
        assertThat(sipStore.find(name.getMethodName()).getState(), equalTo(AipState.ARCHIVED));
        assertThat(xmlStore.find(xmlId).getState(), not(equalTo(XmlState.PROCESSING)));
    }

    @Test
    public void registerXmlUpdate() {
        AipXml xmlEntity = service.registerXmlUpdate(SIP_ID, S);
        assertThat(xmlStore.find(xmlEntity.getId()).getState(), equalTo(XmlState.PROCESSING));
        assertThat(sipStore.find(SIP_ID).getState(), equalTo(AipState.ARCHIVED));
    }

    @Test
    public void registerSipDeletion() throws StillProcessingException, RollbackedException {
        service.registerSipDeletion(SIP_ID);
        assertThat(sipStore.find(SIP_ID).getState(), equalTo(AipState.PROCESSING));
    }

    @Test
    public void finishSipDeletion() throws StillProcessingException, RollbackedException {
        service.registerSipDeletion(SIP_ID);
        service.finishSipDeletion(SIP_ID);
        assertThat(sipStore.find(SIP_ID).getState(), equalTo(AipState.DELETED));
    }

    @Test
    public void finishXmlProcess() {
        AipXml xml = xmlStore.find(XML1_ID);
        xml.setState(XmlState.PROCESSING);
        xmlStore.save(xml);
        service.finishXmlProcess(XML1_ID);
        assertThat(xmlStore.find(XML1_ID).getState(), not(equalTo(XmlState.PROCESSING)));
    }

    @Test
    @Transactional
    public void removeSip() throws DeletedException, StillProcessingException, RollbackedException {
        service.removeSip(SIP_ID);
        AipSip aip = service.getAip(SIP_ID);
        assertThat(aip.getState(), equalTo(AipState.REMOVED));
        assertThat(aip.getXmls(), hasSize(2));
    }

    @Test
    @Transactional
    public void getAip() {
        AipSip aip = service.getAip(SIP_ID);
        assertThat(aip.getState(), equalTo(AipState.ARCHIVED));
        assertThat(aip.getXmls(), hasSize(2));
    }

    @Test
    public void notFoundTest() {
        assertThrown(() -> service.registerSipDeletion(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.registerXmlUpdate(XML1_ID, S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.getAip(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.removeSip(S)).isInstanceOf(MissingObject.class);
    }

    @Test
    public void alreadyExistsTest() {
        assertThrown(() -> service.registerAipCreation(SIP_ID, S, S)).isInstanceOf(ConflictObject.class);
    }

    @Test
    public void nullTest() {
        assertThrown(() -> service.registerAipCreation(null, S, S)).isInstanceOf(BadArgument.class);
        assertThrown(() -> service.registerAipCreation(S, null, S)).isInstanceOf(PersistenceException.class);
        assertThrown(() -> service.registerAipCreation("blah", S, null)).isInstanceOf(PersistenceException.class);
        assertThrown(() -> service.registerXmlUpdate(null, S)).isInstanceOf(BadArgument.class);
        assertThrown(() -> service.registerXmlUpdate(S, null)).isInstanceOf(PersistenceException.class);
    }

    @Test
    public void illegalState() {
        AipSip sip = sipStore.find(SIP_ID);
        sip.setState(AipState.PROCESSING);
        sipStore.save(sip);
        assertThrown(() -> service.registerSipDeletion(SIP_ID)).isInstanceOf(StillProcessingException.class);
        assertThrown(() -> service.removeSip(SIP_ID)).isInstanceOf(StillProcessingException.class);
        sip.setState(AipState.ROLLBACKED);
        sipStore.save(sip);
        assertThrown(() -> service.registerSipDeletion(SIP_ID)).isInstanceOf(RollbackedException.class);
        assertThrown(() -> service.removeSip(SIP_ID)).isInstanceOf(RollbackedException.class);
        sip.setState(AipState.DELETED);
        sipStore.save(sip);
        assertThrown(() -> service.removeSip(SIP_ID)).isInstanceOf(DeletedException.class);
    }

    @Test
    public void findAndDeleteUnfinished() {
        AipSip sip1 = new AipSip("sip1", S, AipState.PROCESSING);
        AipXml xml1 = new AipXml("xml1", S, sip1, 1, XmlState.PROCESSING);
        AipSip sip2 = new AipSip("sip2", S, AipState.ARCHIVED);
        AipXml xml2 = new AipXml("xml2", S, sip2, 1, XmlState.ARCHIVED);
        AipXml xml3 = new AipXml("xml3", S, sip2, 2, XmlState.PROCESSING);
        sipStore.save(sip1);
        sipStore.save(sip2);
        xmlStore.save(xml1);
        xmlStore.save(xml2);
        xmlStore.save(xml3);

        service.rollbackUnfinishedFilesRecords();

        assertThat(sipStore.findUnfinishedSips(), empty());
        assertThat(xmlStore.findUnfinishedXmls(), empty());
        List<AipSip> sips = sipStore.findAll().stream().filter(sip -> sip.getState() != AipState.ROLLBACKED).collect(Collectors.toList());
        List<AipXml> xmls = xmlStore.findAll().stream().filter(xml -> xml.getState() != XmlState.ROLLBACKED).collect(Collectors.toList());
        assertThat(xmls, hasSize(3));
        assertThat(sips, hasSize(2));
    }
}
