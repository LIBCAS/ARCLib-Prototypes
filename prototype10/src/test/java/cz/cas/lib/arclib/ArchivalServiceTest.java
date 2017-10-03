package cz.cas.lib.arclib;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.XmlState;
import cz.cas.lib.arclib.dto.AipCreationMd5Info;
import cz.cas.lib.arclib.exception.DeletedException;
import cz.cas.lib.arclib.exception.RollbackedException;
import cz.cas.lib.arclib.exception.StillProcessingException;
import cz.cas.lib.arclib.service.*;
import cz.cas.lib.arclib.storage.StorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ArchivalServiceTest {

    private static final ArchivalService service = new ArchivalService();
    private static final ArchivalAsyncService async = new ArchivalAsyncService();

    @Mock
    private ArchivalDbService dbService;
    @Mock
    private StorageService storageService;

    private static final String SIP_ID = "SIPtestID";
    private static final String XML1_ID = "XML1testID";
    private static final String XML2_ID = "XML2testID";
    private static final String SIP_HASH = "SIPhash";
    private static final String XML1_HASH = "XML1hash";

    private static final InputStream SIP_STREAM = new ByteArrayInputStream(SIP_ID.getBytes(StandardCharsets.UTF_8));
    private static final InputStream XML1_STREAM = new ByteArrayInputStream(XML1_ID.getBytes(StandardCharsets.UTF_8));
    private static final InputStream XML2_STREAM = new ByteArrayInputStream(XML2_ID.getBytes(StandardCharsets.UTF_8));

    private AipSip sip;
    private AipXml xml1;
    private AipXml xml2;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        service.setArchivalDbService(dbService);
        service.setStorageService(storageService);
        service.setAsyncService(async);

        async.setArchivalDbService(dbService);
        async.setStorageService(storageService);

        sip = new AipSip(SIP_ID, SIP_HASH, AipState.ARCHIVED);
        xml1 = new AipXml(XML1_ID, XML1_HASH, sip, 1, XmlState.ARCHIVED);
        xml2 = new AipXml(XML2_ID, XML2_ID, sip, 2, XmlState.ARCHIVED);
        sip.addXml(xml1);
        sip.addXml(xml2);
    }

    @Test
    public void getAll() throws IOException, DeletedException, StillProcessingException, RollbackedException {
        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getAip(SIP_ID, 1, 2)).thenReturn(Arrays.asList(SIP_STREAM, XML1_STREAM, XML2_STREAM));

        AipRef aip = service.get(SIP_ID, Optional.of(true));

        assertThat(aip.getSip(), equalTo(new FileRef(SIP_ID, SIP_STREAM)));
        assertThat(aip.getXmls(), containsInAnyOrder(new FileRef(XML1_ID, XML1_STREAM), new FileRef(XML2_ID, XML2_STREAM)));
    }

    @Test
    public void getLatest() throws IOException, DeletedException, StillProcessingException, RollbackedException {
        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getAip(eq(SIP_ID), any())).thenReturn(Arrays.asList(SIP_STREAM, XML2_STREAM));

        AipRef aip = service
                .get(SIP_ID, Optional.empty());

        assertThat(aip.getSip(), equalTo(new FileRef(SIP_ID, SIP_STREAM)));
        assertThat(aip.getXmls(), containsInAnyOrder(new FileRef(XML2_ID, XML2_STREAM)));
        assertThat(aip.getXmls().size(), equalTo(1));
    }

    @Test
    public void storeIdProvided() throws IOException {
        when(storageService.storeAip(eq(SIP_STREAM), anyString(), eq(XML1_STREAM))).thenAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        return new AipCreationMd5Info(SIP_HASH, XML1_HASH);
                    }
                }
        );
        when(dbService.registerAipCreation(eq(SIP_ID), eq(SIP_HASH), eq(XML1_HASH))).thenReturn("xmlId");

        service.store(SIP_ID, SIP_STREAM, SIP_HASH, XML1_STREAM, XML1_HASH);

        verify(dbService).registerAipCreation(eq(SIP_ID), eq(SIP_HASH), eq(XML1_HASH));
        verify(storageService).storeAip(SIP_STREAM, SIP_ID, XML1_STREAM);
        verify(dbService).finishAipCreation(SIP_ID, "xmlId");
    }

    @Test
    public void storeBadMD5() throws IOException {
        when(storageService.storeAip(eq(SIP_STREAM), anyString(), eq(XML1_STREAM))).thenAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        return new AipCreationMd5Info("wrongMD5", XML1_HASH);
                    }
                }
        );
        when(dbService.registerAipCreation(eq(SIP_ID), eq(SIP_HASH), eq(XML1_HASH))).thenReturn("xmlId");

        service.store(SIP_ID, SIP_STREAM, SIP_HASH, XML1_STREAM, XML1_HASH);

        verify(dbService).registerAipCreation(eq(SIP_ID), eq(SIP_HASH), eq(XML1_HASH));
        verify(storageService).storeAip(eq(SIP_STREAM), eq(SIP_ID), eq(XML1_STREAM));
        verify(storageService).deleteSip(eq(SIP_ID), eq(true));
        verify(storageService).deleteXml(eq(SIP_ID), eq(1));
        verify(dbService).rollbackSip(eq(SIP_ID), eq("xmlId"));
    }

    @Test
    public void updateXml() throws IOException {
        AipXml xmlEntity = new AipXml("xmlId", XML1_HASH, null, 50, XmlState.PROCESSING);
        when(storageService.storeXml(eq(XML1_STREAM), anyString(), eq(50))).thenReturn(XML1_HASH);
        when(dbService.registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH))).thenReturn(xmlEntity);

        service.updateXml(SIP_ID, XML1_STREAM, XML1_HASH);

        verify(dbService).registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH));
        verify(storageService).storeXml(eq(XML1_STREAM), eq(SIP_ID), eq(50));
        verify(dbService).finishXmlProcess("xmlId");
    }

    @Test
    public void updateXmlBadMD5() throws IOException {
        AipXml xmlEntity = new AipXml("xmlId", XML1_HASH, null, 51, XmlState.PROCESSING);
        when(storageService.storeXml(eq(XML1_STREAM), anyString(), anyInt())).thenReturn("wrongmd5");
        when(dbService.registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH))).thenReturn(xmlEntity);

        service.updateXml(SIP_ID, XML1_STREAM, XML1_HASH);

        verify(dbService).registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH));
        verify(storageService).storeXml(eq(XML1_STREAM), eq(SIP_ID), eq(51));
        verify(storageService).deleteXml(eq(SIP_ID), eq(51));
        verify(dbService).rollbackXml(eq("xmlId"));
    }

    @Test
    public void getAipInfo() throws IOException, StillProcessingException {
        Map<Integer, String> checksums = new HashMap<>();
        checksums.put(1, XML1_HASH);
        checksums.put(2, "wronghash");

        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getXmlsMD5(SIP_ID, Arrays.asList(1, 2))).thenReturn(checksums);
        when(storageService.getSipMD5(eq(SIP_ID))).thenReturn(SIP_HASH);

        AipSip res = service.getAipState(SIP_ID);

        assertThat(res.isConsistent(), is(true));
        assertThat(res.getState(), is(AipState.ARCHIVED));
        assertThat(res.getXml(0).isConsistent(), not(equalTo(res.getXml(1).isConsistent())));
    }
}
