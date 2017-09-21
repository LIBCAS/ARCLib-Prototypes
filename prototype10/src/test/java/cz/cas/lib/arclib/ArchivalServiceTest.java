package cz.cas.lib.arclib;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.dto.StoredFileInfoDto;
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
import java.util.List;
import java.util.Map;

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
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

        sip = new AipSip(SIP_ID, SIP_ID, SIP_HASH, AipState.ARCHIVED);
        xml1 = new AipXml(XML1_ID, XML1_ID, XML1_HASH, sip, 1, false);
        xml2 = new AipXml(XML2_ID, XML2_ID, XML2_ID, sip, 2, false);
        sip.addXml(xml1);
        sip.addXml(xml2);
    }

    @Test
    public void get() throws IOException, NotFound {
        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getAip(SIP_ID, XML1_ID, XML2_ID)).thenReturn(Arrays.asList(SIP_STREAM, XML1_STREAM, XML2_STREAM));

        AipRef aip = service.get(SIP_ID);

        assertThat(aip.getSip(), equalTo(new FileRef(SIP_ID, SIP_ID, SIP_STREAM)));
        assertThat(aip.getXmls(), containsInAnyOrder(new FileRef(XML1_ID, XML1_ID, XML1_STREAM), new FileRef(XML2_ID, XML2_ID, XML2_STREAM)));
    }

    @Test
    public void store() throws IOException, ChecksumChanged {

        when(storageService.storeAip(eq(SIP_STREAM), anyString(), eq(XML1_STREAM), anyString())).thenAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        Map<String, String> checksums = new HashMap<>();
                        checksums.put((String) invocation.getArguments()[1], SIP_HASH);
                        checksums.put((String) invocation.getArguments()[3], XML1_HASH);
                        return checksums;
                    }
                }
        );

        List<StoredFileInfoDto> res = service.store(SIP_STREAM, SIP_ID, SIP_HASH, XML1_STREAM, XML1_ID, XML1_HASH);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerAipCreation(argument.capture(), anyString(), eq(SIP_HASH), argument.capture(), anyString(), eq(XML1_HASH));
        String generatedSipId = argument.getAllValues().get(0);
        String generatedXmlId = argument.getAllValues().get(1);

        verify(storageService).storeAip(SIP_STREAM, generatedSipId, XML1_STREAM, generatedXmlId);
        assertThat(generatedSipId, not(equalTo(generatedXmlId)));
        verify(dbService).finishAipCreation(generatedSipId, generatedXmlId);
        assertThat(res.get(0), equalTo(new StoredFileInfoDto(generatedSipId, SIP_ID)));
        assertThat(res.get(1), equalTo(new StoredFileInfoDto(generatedXmlId, XML1_ID)));
    }

    @Test
    public void storeBadMD5() throws IOException {
        when(storageService.storeAip(eq(SIP_STREAM), anyString(), eq(XML1_STREAM), anyString())).thenAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        Map<String, String> checksums = new HashMap<>();
                        checksums.put((String) invocation.getArguments()[1], "wrongMD5");
                        checksums.put((String) invocation.getArguments()[3], XML1_HASH);
                        return checksums;
                    }
                }
        );

        assertThrown(() -> service.store(SIP_STREAM, SIP_ID, SIP_HASH, XML1_STREAM, XML1_ID, XML1_HASH)).isInstanceOf(ChecksumChanged.class);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerAipCreation(argument.capture(), anyString(), eq(SIP_HASH), argument.capture(), anyString(), eq(XML1_HASH));
        String generatedSipId = argument.getAllValues().get(0);
        String generatedXmlId = argument.getAllValues().get(1);

        verify(storageService).storeAip(SIP_STREAM, generatedSipId, XML1_STREAM, generatedXmlId);
        assertThat(generatedSipId, not(equalTo(generatedXmlId)));
        verify(storageService).delete(generatedSipId);
        verify(storageService).delete(generatedXmlId);
        verify(dbService).deleteAip(generatedSipId);
    }

    @Test
    public void updateXml() throws IOException, ChecksumChanged {
        when(storageService.storeXml(eq(XML1_STREAM), anyString())).thenReturn(XML1_HASH);

        StoredFileInfoDto res = service.updateXml(SIP_ID, XML1_ID, XML1_STREAM, XML1_HASH);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerXmlUpdate(eq(SIP_ID), argument.capture(), anyString(), eq(XML1_HASH));
        String generatedXmlId = argument.getValue();
        verify(storageService).storeXml(XML1_STREAM, generatedXmlId);
        verify(dbService).finishXmlProcess(generatedXmlId);
        assertThat(res, equalTo(new StoredFileInfoDto(generatedXmlId, XML1_ID)));
    }

    @Test
    public void updateXmlBadMD5() throws IOException {
        when(storageService.storeXml(eq(XML1_STREAM), anyString())).thenReturn("wrongmd5");

        assertThrown(() -> service.updateXml(SIP_ID, XML1_ID, XML1_STREAM, XML1_HASH)).isInstanceOf(ChecksumChanged.class);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerXmlUpdate(eq(SIP_ID), argument.capture(), anyString(), eq(XML1_HASH));
        String generatedXmlId = argument.getValue();
        verify(storageService).storeXml(XML1_STREAM, generatedXmlId);
        verify(storageService).delete(generatedXmlId);
        verify(dbService).deleteXml(generatedXmlId);
    }

    @Test
    public void getAipInfo() throws IOException {
        Map<String, String> checksums = new HashMap<>();
        checksums.put(SIP_ID, SIP_HASH);
        checksums.put(XML1_ID, XML1_HASH);
        checksums.put(XML2_ID, "wronghash");

        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getMD5(SIP_ID, Arrays.asList(XML1_ID, XML2_ID))).thenReturn(checksums);

        AipSip res = service.getAipInfo(SIP_ID);

        assertThat(res.isConsistent(), is(true));
        assertThat(res.getState(), is(AipState.ARCHIVED));
        assertThat(res.getXml(0).isConsistent(), not(equalTo(res.getXml(1).isConsistent())));
    }


}
