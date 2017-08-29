package cz.inqool.arclib;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.dto.StoredFileInfoDto;
import cz.inqool.arclib.service.AipRef;
import cz.inqool.arclib.service.ArchivalDbService;
import cz.inqool.arclib.service.ArchivalService;
import cz.inqool.arclib.service.FileRef;
import cz.inqool.arclib.storage.StorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
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

    private static final InputStream META1_STREAM = new ByteArrayInputStream((SIP_HASH + System.lineSeparator() + XML1_HASH).getBytes(StandardCharsets.UTF_8));
    private static final InputStream META2_STREAM = new ByteArrayInputStream(XML1_HASH.getBytes(StandardCharsets.UTF_8));

    private AipSip sip;
    private AipXml xml1;
    private AipXml xml2;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        service.setArchivalDbService(dbService);
        service.setStorageService(storageService);

        sip = new AipSip(SIP_ID, SIP_ID, SIP_ID, AipState.ARCHIVED);
        xml1 = new AipXml(XML1_ID, XML1_ID, XML1_ID, sip, 1, false);
        xml2 = new AipXml(XML2_ID, XML2_ID, XML2_ID, sip, 2, false);
        sip.addXml(xml1);
        sip.addXml(xml2);
    }

    @Test
    public void get() throws IOException {
        when(dbService.getAip(SIP_ID)).thenReturn(sip);
        when(storageService.getAip(SIP_ID, XML1_ID, XML2_ID)).thenReturn(Arrays.asList(SIP_STREAM, XML1_STREAM, XML2_STREAM));

        AipRef aip = service.get(SIP_ID);

        assertThat(aip.getSip(), equalTo(new FileRef(SIP_ID, SIP_ID, SIP_STREAM)));
        assertThat(aip.getXmls(), containsInAnyOrder(new FileRef(XML1_ID, XML1_ID, XML1_STREAM), new FileRef(XML2_ID, XML2_ID, XML2_STREAM)));
    }

    @Test
    public void store() throws IOException {
        when(storageService.storeAip(eq(SIP_STREAM),anyString(),eq(XML1_STREAM),anyString())).thenAnswer(
                new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        Map<String,String> checksums = new HashMap<>();
                        checksums.put((String)invocation.getArguments()[1],"wrongMD5");
                        checksums.put((String)invocation.getArguments()[3],XML1_HASH);
                        return checksums;
                    }
                }
        );

        List<StoredFileInfoDto> res = service.store(SIP_STREAM, SIP_ID, XML1_STREAM, XML1_ID, META1_STREAM);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerAipCreation(argument.capture(), anyString(), eq(SIP_HASH), argument.capture(), anyString(), eq(XML1_HASH));
        String generatedSipId = argument.getAllValues().get(0);
        String generatedXmlId = argument.getAllValues().get(1);

        verify(storageService).storeAip(SIP_STREAM, generatedSipId, XML1_STREAM, generatedXmlId);
        assertThat(generatedSipId, not(equalTo(generatedXmlId)));
        verify(dbService).finishAipCreation(generatedSipId, generatedXmlId);
        assertThat(res.get(0), equalTo(new StoredFileInfoDto(generatedSipId, SIP_ID, false)));
        assertThat(res.get(1), equalTo(new StoredFileInfoDto(generatedXmlId, XML1_ID, true)));
    }

    @Test
    public void updateXml() throws IOException {
        when(storageService.storeXml(eq(XML1_STREAM),anyString())).thenReturn("wrongmd5");

        StoredFileInfoDto res = service.updateXml(SIP_ID, XML1_ID, XML1_STREAM, META2_STREAM);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(dbService).registerXmlUpdate(eq(SIP_ID), argument.capture(), anyString(), eq(XML1_HASH));
        String generatedXmlId = argument.getValue();
        verify(storageService).storeXml(XML1_STREAM, generatedXmlId);
        verify(dbService).finishXmlProcess(generatedXmlId);
        assertThat(res, equalTo(new StoredFileInfoDto(generatedXmlId, XML1_ID, false)));
    }
}
