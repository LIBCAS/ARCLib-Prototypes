package cz.cas.lib.arclib;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.dto.AipCreationMd5Info;
import cz.cas.lib.arclib.exception.ChecksumChanged;
import cz.cas.lib.arclib.exception.NotFound;
import cz.cas.lib.arclib.service.AipRef;
import cz.cas.lib.arclib.service.ArchivalDbService;
import cz.cas.lib.arclib.service.ArchivalService;
import cz.cas.lib.arclib.service.FileRef;
import cz.cas.lib.arclib.storage.StorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ArchivalServiceTest {

    private static final ArchivalService service = new ArchivalService();

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

        sip = new AipSip(SIP_ID, SIP_HASH, AipState.ARCHIVED);
        xml1 = new AipXml(XML1_ID, XML1_HASH, sip, 1, false);
        xml2 = new AipXml(XML2_ID, XML2_ID, sip, 2, false);
        sip.addXml(xml1);
        sip.addXml(xml2);
    }

    @Test
    public void getAll() throws IOException, NotFound {
        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getAip(SIP_ID, 1, 2)).thenReturn(Arrays.asList(SIP_STREAM, XML1_STREAM, XML2_STREAM));

        AipRef aip = service.get(SIP_ID, Optional.of(true));

        assertThat(aip.getSip(), equalTo(new FileRef(SIP_ID, SIP_STREAM)));
        assertThat(aip.getXmls(), containsInAnyOrder(new FileRef(XML1_ID, XML1_STREAM), new FileRef(XML2_ID, XML2_STREAM)));
    }

    @Test
    public void getLatest() throws IOException, NotFound {
        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getAip(eq(SIP_ID), any())).thenReturn(Arrays.asList(SIP_STREAM, XML2_STREAM));

        AipRef aip = service.get(SIP_ID, Optional.empty());

        assertThat(aip.getSip(), equalTo(new FileRef(SIP_ID, SIP_STREAM)));
        assertThat(aip.getXmls(), containsInAnyOrder(new FileRef(XML2_ID, XML2_STREAM)));
        assertThat(aip.getXmls().size(), equalTo(1));
    }

    @Test
    public void storeIdProvided() throws IOException, ChecksumChanged {
        when(storageService.storeAip(eq(SIP_STREAM), anyString(), eq(XML1_STREAM))).thenAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        return new AipCreationMd5Info(SIP_HASH, XML1_HASH);
                    }
                }
        );
        when(dbService.registerAipCreation(eq(SIP_ID), eq(SIP_HASH), eq(XML1_HASH))).thenReturn("xmlId");

        String res = service.store(SIP_STREAM, SIP_HASH, XML1_STREAM, XML1_HASH, Optional.of(SIP_ID));

        verify(dbService).registerAipCreation(eq(SIP_ID), eq(SIP_HASH), eq(XML1_HASH));
        verify(storageService).storeAip(SIP_STREAM, SIP_ID, XML1_STREAM);
        verify(dbService).finishAipCreation(SIP_ID, "xmlId");
        assertThat(res, equalTo(SIP_ID));
    }

    @Test
    public void storeIdNotProvided() throws IOException, ChecksumChanged {
        when(storageService.storeAip(eq(SIP_STREAM), anyString(), eq(XML1_STREAM))).thenAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        return new AipCreationMd5Info(SIP_HASH, XML1_HASH);
                    }
                }
        );
        when(dbService.registerAipCreation(anyString(), eq(SIP_HASH), eq(XML1_HASH))).thenReturn("xmlId");

        String res = service.store(SIP_STREAM, SIP_HASH, XML1_STREAM, XML1_HASH, Optional.empty());

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerAipCreation(argument.capture(), eq(SIP_HASH), eq(XML1_HASH));
        String generatedSipId = argument.getAllValues().get(0);
        verify(storageService).storeAip(SIP_STREAM, generatedSipId, XML1_STREAM);
        verify(dbService).finishAipCreation(generatedSipId, "xmlId");
        assertThat(res, equalTo(generatedSipId));
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

        assertThrown(() -> service.store(SIP_STREAM, SIP_HASH, XML1_STREAM, XML1_HASH, Optional.of(SIP_ID))).isInstanceOf(ChecksumChanged.class);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerAipCreation(eq(SIP_ID), eq(SIP_HASH), eq(XML1_HASH));
        verify(storageService).storeAip(eq(SIP_STREAM), eq(SIP_ID), eq(XML1_STREAM));
        verify(storageService).deleteSip(eq(SIP_ID));
        verify(storageService).deleteXml(eq(SIP_ID), eq(1));
        verify(dbService).deleteAip(eq(SIP_ID));
    }

    @Test
    public void updateXml() throws IOException, ChecksumChanged {
        AipXml xmlEntity = new AipXml("xmlId", XML1_HASH, null, 50, true);
        when(storageService.storeXml(eq(XML1_STREAM), anyString(), eq(50))).thenReturn(XML1_HASH);
        when(dbService.registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH))).thenReturn(xmlEntity);

        service.updateXml(SIP_ID, XML1_STREAM, XML1_HASH);

        verify(dbService).registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH));
        verify(storageService).storeXml(eq(XML1_STREAM), eq(SIP_ID), eq(50));
        verify(dbService).finishXmlProcess("xmlId");
    }

    @Test
    public void updateXmlBadMD5() throws IOException {
        AipXml xmlEntity = new AipXml("xmlId", XML1_HASH, null, 51, true);
        when(storageService.storeXml(eq(XML1_STREAM), anyString(), anyInt())).thenReturn("wrongmd5");
        when(dbService.registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH))).thenReturn(xmlEntity);

        assertThrown(() -> service.updateXml(SIP_ID, XML1_STREAM, XML1_HASH)).isInstanceOf(ChecksumChanged.class);

        verify(dbService).registerXmlUpdate(eq(SIP_ID), eq(XML1_HASH));
        verify(storageService).storeXml(eq(XML1_STREAM), eq(SIP_ID), eq(51));
        verify(storageService).deleteXml(eq(SIP_ID), eq(51));
        verify(dbService).deleteXml(eq("xmlId"));
    }

    @Test
    public void getAipInfo() throws IOException {
        Map<Integer, String> checksums = new HashMap<>();
        checksums.put(1, XML1_HASH);
        checksums.put(2, "wronghash");

        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getXmlsMD5(SIP_ID, Arrays.asList(1, 2))).thenReturn(checksums);
        when(storageService.getSipMD5(eq(SIP_ID))).thenReturn(SIP_HASH);

        AipSip res = service.getAipInfo(SIP_ID);

        assertThat(res.isConsistent(), is(true));
        assertThat(res.getState(), is(AipState.ARCHIVED));
        assertThat(res.getXml(0).isConsistent(), not(equalTo(res.getXml(1).isConsistent())));
    }


}
