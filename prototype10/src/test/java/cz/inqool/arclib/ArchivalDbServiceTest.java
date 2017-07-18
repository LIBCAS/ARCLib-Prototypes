package cz.inqool.arclib;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.helper.DbTest;
import cz.inqool.arclib.service.ArchivalDbService;
import cz.inqool.arclib.store.AipSipStore;
import cz.inqool.arclib.store.AipXmlStore;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.ConflictObject;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.store.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.persistence.PersistenceException;
import java.sql.SQLException;

import static cz.inqool.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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

        AipSip sip = new AipSip(SIP_ID, S, S, AipState.ARCHIVED);
        sipStore.save(sip);
        xmlStore.save(new AipXml(XML1_ID, S, S, sip, 1, false));
        xmlStore.save(new AipXml(XML2_ID, S, S, sip, 2, false));
    }

    @After
    public void after() throws SQLException {
        clearDatabase();

    }

    @Test
    public void registerAipCreation() {
        service.registerAipCreation(name.getMethodName(), S, S, name.getMethodName(), S, S);
        assertThat(sipStore.find(name.getMethodName()).getState(), equalTo(AipState.PROCESSING));
        assertThat(xmlStore.find(name.getMethodName()).isProcessing(), equalTo(true));
    }

    @Test
    public void finishAipCreation() {
        service.registerAipCreation(name.getMethodName(), S, S, name.getMethodName(), S, S);
        service.finishAipCreation(name.getMethodName(), name.getMethodName());
        assertThat(sipStore.find(name.getMethodName()).getState(), equalTo(AipState.ARCHIVED));
        assertThat(xmlStore.find(name.getMethodName()).isProcessing(), equalTo(false));
    }

    @Test
    public void registerXmlUpdate() {
        service.registerXmlUpdate(SIP_ID, name.getMethodName(), S, S);
        assertThat(xmlStore.find(name.getMethodName()).isProcessing(), equalTo(true));
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
        assertThrown(() -> service.registerXmlUpdate(XML1_ID, name.getMethodName(), S, S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.getAip(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.removeSip(S)).isInstanceOf(MissingObject.class);
        assertThrown(() -> service.finishXmlProcess(S)).isInstanceOf(MissingObject.class);
    }

    @Test
    public void alreadyExistsTest() {
        assertThrown(() -> service.registerAipCreation(SIP_ID, SIP_ID, SIP_ID, name.getMethodName(), XML1_ID, XML1_ID)).isInstanceOf(ConflictObject.class);
        assertThrown(() -> service.registerAipCreation(name.getMethodName(), SIP_ID, SIP_ID, XML1_ID, XML1_ID, XML1_ID)).isInstanceOf(ConflictObject.class);
        assertThrown(() -> service.registerXmlUpdate(SIP_ID, XML1_ID, S, S)).isInstanceOf(ConflictObject.class);
    }

    @Test
    public void nullTest() {
        assertThrown(() -> service.registerAipCreation(null, S, S, S, S, S)).isInstanceOf(BadArgument.class);
        assertThrown(() -> service.registerAipCreation(S, null, S, S, S, S)).isInstanceOf(PersistenceException.class);
        assertThrown(() -> service.registerAipCreation(S, S, null, S, S, S)).isInstanceOf(PersistenceException.class);
        assertThrown(() -> service.registerAipCreation(S, S, S, null, S, S)).isInstanceOf(BadArgument.class);
        assertThrown(() -> service.registerAipCreation(S, S, S, S, null, S)).isInstanceOf(PersistenceException.class);
        assertThrown(() -> service.registerAipCreation(S, S, S, S, S, null)).isInstanceOf(PersistenceException.class);
        assertThrown(() -> service.registerXmlUpdate(null, S, S, S)).isInstanceOf(BadArgument.class);
        assertThrown(() -> service.registerXmlUpdate(S, null, S, S)).isInstanceOf(BadArgument.class);
        assertThrown(() -> service.registerXmlUpdate(S, S, null, S)).isInstanceOf(PersistenceException.class);
        assertThrown(() -> service.registerXmlUpdate(S, S, S, null)).isInstanceOf(PersistenceException.class);
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

}
