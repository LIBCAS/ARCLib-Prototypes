package cz.cas.lib.arclib;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.exception.ConflictObject;
import cz.cas.lib.arclib.exception.MissingObject;
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
    private static final String S = "";

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
        xmlStore.save(new AipXml(XML1_ID, S, sip, 1, false));
        xmlStore.save(new AipXml(XML2_ID, S, sip, 2, false));
    }

    @After
    public void after() throws SQLException {
        clearDatabase();
    }

    @Test
    public void registerAipCreation() {
        String xmlId = service.registerAipCreation(name.getMethodName(), S, S);
        assertThat(sipStore.find(name.getMethodName()).getState(), equalTo(AipState.PROCESSING));
        assertThat(xmlStore.find(xmlId).isProcessing(), equalTo(true));
    }

    @Test
    public void finishAipCreation() {
        String xmlId = service.registerAipCreation(name.getMethodName(), S, S);
        service.finishAipCreation(name.getMethodName(), xmlId);
        assertThat(sipStore.find(name.getMethodName()).getState(), equalTo(AipState.ARCHIVED));
        assertThat(xmlStore.find(xmlId).isProcessing(), equalTo(false));
    }

    @Test
    public void registerXmlUpdate() {
        AipXml xmlEntity = service.registerXmlUpdate(SIP_ID, S);
        assertThat(xmlStore.find(xmlEntity.getId()).isProcessing(), equalTo(true));
        assertThat(sipStore.find(SIP_ID).getState(), equalTo(AipState.ARCHIVED));
    }

    @Test
    public void registerSipDeletion() {
        service.registerSipDeletion(SIP_ID);
        assertThat(sipStore.find(SIP_ID).getState(), equalTo(AipState.PROCESSING));
    }

    @Test
    public void finishSipDeletion() {
        service.registerSipDeletion(SIP_ID);
        service.finishSipDeletion(SIP_ID);
        assertThat(sipStore.find(SIP_ID).getState(), equalTo(AipState.DELETED));
    }

    @Test
    public void finishXmlProcess() {
        AipXml xml = xmlStore.find(XML1_ID);
        xml.setProcessing(true);
        xmlStore.save(xml);
        service.finishXmlProcess(XML1_ID);
        assertThat(xmlStore.find(XML1_ID).isProcessing(), equalTo(false));
    }

    @Test
    @Transactional
    public void removeSip() {
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
        assertThrown(() -> service.finishAipCreation(S, S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.registerSipDeletion(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.finishSipDeletion(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.registerXmlUpdate(XML1_ID, S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.getAip(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.removeSip(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.finishXmlProcess(S)).isInstanceOf(MissingObject.class);
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
        assertThrown(() -> service.finishSipDeletion(SIP_ID)).isInstanceOf(IllegalStateException.class);
        assertThrown(() -> service.finishAipCreation(SIP_ID, XML1_ID)).isInstanceOf(IllegalStateException.class);
        assertThrown(() -> service.finishXmlProcess(XML1_ID)).isInstanceOf(IllegalStateException.class);
        AipSip sip = sipStore.find(SIP_ID);
        sip.setState(AipState.PROCESSING);
        sipStore.save(sip);
        assertThrown(() -> service.registerSipDeletion(SIP_ID)).isInstanceOf(IllegalStateException.class);
        assertThrown(() -> service.removeSip(SIP_ID)).isInstanceOf(IllegalStateException.class);
        sip.setState(AipState.DELETED);
        sipStore.save(sip);
        assertThrown(() -> service.removeSip(SIP_ID)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void findAndDeleteUnfinished() {
        AipSip sip1 = new AipSip("sip1", S, AipState.PROCESSING);
        AipXml xml1 = new AipXml("xml1", S, sip1, 1, true);
        AipSip sip2 = new AipSip("sip2", S, AipState.ARCHIVED);
        AipXml xml2 = new AipXml("xml2", S, sip2, 1, false);
        AipXml xml3 = new AipXml("xml3", S, sip2, 2, true);
        sipStore.save(sip1);
        sipStore.save(sip2);

        xmlStore.save(xml1);
        xmlStore.save(xml2);
        xmlStore.save(xml3);

        List<AipSip> sips = sipStore.findUnfinishedSips();
        List<AipXml> xmls = xmlStore.findUnfinishedXmls();

        assertThat(sips, hasSize(1));
        assertThat(sips.get(0), equalTo(sip1));
        assertThat(xmls, hasSize(1));
        assertThat(xmls.get(0), equalTo(xml3));

        sipStore.deleteUnfinishedSipsRecords();
        xmlStore.deleteUnfinishedXmlsRecords();

        assertThat(sipStore.findUnfinishedSips(), empty());
        assertThat(xmlStore.findUnfinishedXmls(), empty());
        assertThat(xmlStore.findAll(), hasSize(3));
        assertThat(sipStore.findAll(), hasSize(2));
    }
}
