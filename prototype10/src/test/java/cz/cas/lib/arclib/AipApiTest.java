package cz.cas.lib.arclib;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.api.AipApi;
import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.XmlState;
import cz.cas.lib.arclib.helper.ApiTest;
import cz.cas.lib.arclib.helper.DbTest;
import cz.cas.lib.arclib.storage.StorageType;
import cz.cas.lib.arclib.store.AipSipStore;
import cz.cas.lib.arclib.store.AipXmlStore;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * If null pointer exception is thrown inside test the cause can be that async method invocation of previous test has
 * not finished during that test. {@link Thread#sleep(long)} is used to overcome this.
 * Most of POST and DELETE requests run asynchronously.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AipApiTest extends DbTest implements ApiTest {

    @Inject
    private AipApi api;

    @Inject
    private AipSipStore sipStore;
    @Inject
    private AipXmlStore xmlStore;

    private static final String SIP_ID = "8f719ff7-8756-4101-9e87-42391ced37f1";
    private static final String SIP_HASH = "CB30ACE944A440F77D6F99040D2DE1F2";

    private static final String XML1_ID = "XML1testID";
    private static final String XML2_ID = "XML2testID";

    private static final String XML1_HASH = "F09E5F27526A0ED7EC5B2D9D5C0B53CF";
    private static final String XML2_HASH = "D5B6402517014CF00C223D6A785A4230";

    private static final Path SIP_PATH_DIRS = Paths.get(SIP_ID.substring(0, 2), SIP_ID.substring(2, 4), SIP_ID.substring(4, 6));
    private static final Path SIP_PATH = Paths.get("sip").resolve(SIP_PATH_DIRS);
    private static final Path XMLS_PATH = Paths.get("xml").resolve(SIP_PATH_DIRS);

    private static final String BASE = "/api/storage";
    private static final Path SIP_SOURCE_PATH = Paths.get("..", "KPW01169310.ZIP");

    /**
     * Recursively deletes old test directories, create new and copies one SIP and two XMLs to them.
     * Creates new database records before every test
     */
    @Before
    public void before() throws IOException {
        xmlStore.setEntityManager(getEm());
        xmlStore.setQueryFactory(new JPAQueryFactory(getEm()));

        sipStore.setEntityManager(getEm());
        sipStore.setQueryFactory(new JPAQueryFactory(getEm()));

        AipSip sip = new AipSip(SIP_ID, SIP_HASH, AipState.ARCHIVED);
        sipStore.save(sip);
        xmlStore.save(new AipXml(XML1_ID, XML1_HASH, sip, 1, XmlState.ARCHIVED));
        xmlStore.save(new AipXml(XML2_ID, XML2_HASH, sip, 2, XmlState.ARCHIVED));

        if (Files.isDirectory(Paths.get("sip")))
            FileUtils.deleteDirectory(new File("sip"));
        if (Files.isDirectory(Paths.get("xml")))
            FileUtils.deleteDirectory(new File("xml"));
        Files.createDirectories(SIP_PATH);
        Files.createDirectories(XMLS_PATH);
        Files.copy(Paths.get(SIP_SOURCE_PATH.toString()), SIP_PATH.resolve(SIP_ID));
        Files.copy(Paths.get("./src/test/resources/aip/xml1.xml"), XMLS_PATH.resolve(toXmlId(SIP_ID, 1)));
        Files.copy(Paths.get("./src/test/resources/aip/xml2.xml"), XMLS_PATH.resolve(toXmlId(SIP_ID, 2)));
    }

    /**
     * Clear database records after each test
     *
     * @throws SQLException
     */
    @After
    public void after() throws SQLException, InterruptedException {
        clearDatabase();
    }

    /**
     * Send request for AIP data and verifies that ZIP file containing one ZIP (SIP) and latest AIP XML is retrieved.
     *
     * @throws Exception
     */
    @Test
    public void getAip() throws Exception {
        byte[] zip = mvc(api)
                .perform(get(BASE + "/{sipId}", SIP_ID))
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=aip_" + SIP_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsByteArray();
        List<String> packedFiles = new ArrayList<>();
        try (ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                packedFiles.add(entry.getName());
            }
        }
        assertThat(packedFiles, containsInAnyOrder(SIP_ID, toXmlId(SIP_ID, 2)));
    }

    /**
     * Send AIP creation request with AIP data (sip & xml) and verifies that response contains ID of newly created AIP (SIP).
     * Then send state request and verifies that SIP exists.
     *
     * @throws Exception
     */
    @Test
    public void saveIdProvided() throws Exception {
        MockMultipartFile sipFile = new MockMultipartFile(
                "sip", "sip", "text/plain", Files.readAllBytes(SIP_SOURCE_PATH));
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML1_ID.getBytes());
        String id = "1ced37f1-8756-4101-9e87-42398f719ff7";
        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/store").file(sipFile).file(xmlFile).param("sipMD5", SIP_HASH).param("xmlMD5", XML1_HASH).param("UUID", id))
                .andExpect(status().isOk());
        assertThat(id, not(isEmptyOrNullString()));
        Thread.sleep(4000);
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ARCHIVED"))
                .andExpect(jsonPath("$.consistent").value(true));
    }

    /**
     * Send AIP creation request with AIP data where MD5 param does not match MD5 of a file.
     * Then send request for AIP state and verifies that both XML and SIP are ROLLBACKED.
     * Then send request for AIP data and verifies 500 response code.
     *
     * @throws Exception
     */
    @Test
    public void saveMD5Changed() throws Exception {
        MockMultipartFile sipFile = new MockMultipartFile(
                "sip", "sip", "text/plain", Files.readAllBytes(SIP_SOURCE_PATH));
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML1_ID.getBytes());
        String sipId = mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/store").file(sipFile).file(xmlFile).param("sipMD5", XML1_HASH).param("xmlMD5", XML1_HASH))
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();
        Thread.sleep(6000);
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", sipId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(false))
                .andExpect(jsonPath("$.state").value("ROLLBACKED"))
                .andExpect(jsonPath("$.xmls[0].state").value("ROLLBACKED"))
                .andExpect(jsonPath("$.xmls[0].consistent").value(false));
        mvc(api)
                .perform(get(BASE + "/{sipId}", sipId))
                .andExpect(status().is(500));
    }

    /**
     * Send get AIP request and while it is being processed
     * send requests for AIP data and state which both returns 423 "LOCKED" state.
     *
     * @throws Exception
     */
    @Test
    public void getDuringSave() throws Exception {
        MockMultipartFile sipFile = new MockMultipartFile(
                "sip", "sip", "text/plain", Files.readAllBytes(SIP_SOURCE_PATH));
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML1_ID.getBytes());
        String sipId = mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/store").file(sipFile).file(xmlFile).param("sipMD5", SIP_HASH).param("xmlMD5", XML1_HASH))
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", sipId))
                .andExpect(status().isLocked());
        mvc(api)
                .perform(get(BASE + "/{sipId}", sipId))
                .andExpect(status().isLocked());
        Thread.sleep(4000);
    }

    /**
     * Send request for latest xml and verifies data in response.
     *
     * @throws Exception
     */
    @Test
    public void getLatestXml() throws Exception {
        byte[] xmlContent = mvc(api)
                .perform(get(BASE + "/xml/{sipId}", SIP_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsByteArray();
        assertThat(xmlContent, equalTo(XML2_ID.getBytes()));
    }

    /**
     * Send request for xml specified by version and verifies data in response.
     *
     * @throws Exception
     */
    @Test
    public void getXmlByVersion() throws Exception {
        byte[] xmlContent = mvc(api)
                .perform(get(BASE + "/xml/{sipId}", SIP_ID).param("v", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse()
                .getContentAsByteArray();
        assertThat(xmlContent, equalTo(XML1_ID.getBytes()));
    }

    /**
     * Send request for XML update.
     * Then sends request for AIP state and verifies new XML record is there.
     *
     * @throws Exception
     */
    @Test
    public void updateXml() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());
        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).param("xmlMD5", XML2_HASH))
                .andExpect(status().isOk());
        Thread.sleep(2000);
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xmls[2].state").value("ARCHIVED"))
                .andExpect(jsonPath("$.xmls[2].consistent").value(true));
    }

    /**
     * Send request for XML update where MD5 param does not match MD5 of a file.
     * Then send request for AIP state and verifies that only last XML is ROLLBACKED.
     * Then send request for XML data and verifies 500 response code.
     *
     * @throws Exception
     */
    @Test
    public void updateXmlMD5Changed() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());
        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).param("xmlMD5", SIP_HASH))
                .andExpect(status().is(200));
        Thread.sleep(4000);
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(true))
                .andExpect(jsonPath("$.state").value("ARCHIVED"))
                .andExpect(jsonPath("$.xmls[0].state").value("ARCHIVED"))
                .andExpect(jsonPath("$.xmls[0].consistent").value(true))
                .andExpect(jsonPath("$.xmls[2].state").value("ROLLBACKED"))
                .andExpect(jsonPath("$.xmls[2].consistent").value(false));
        mvc(api)
                .perform(get(BASE + "/xml/{sipId}", SIP_ID))
                .andExpect(status().is(500));
    }

    /**
     * Send get XML request and while it is being processed:
     * send requests for XML data and verifies 423 "LOCKED" state
     * send request for AIP state and verifies XML is in state PROCESSING
     *
     * @throws Exception
     */
    @Test
    public void getDuringXmlUpdate() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());
        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).param("xmlMD5", XML2_HASH))
                .andExpect(status().is(200));
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xmls[2].state").value("PROCESSING"));
        mvc(api)
                .perform(get(BASE + "/xml/{sipId}", SIP_ID))
                .andExpect(status().isLocked());
        Thread.sleep(2000);
    }

    /**
     * Sends AIP remove (soft delete) request then sends AIP state request and verifies state change.
     *
     * @throws Exception
     */
    @Test
    public void remove() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}", SIP_ID))
                .andExpect(status().isOk());
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(true))
                .andExpect(jsonPath("$.state").value("REMOVED"));
    }

    /**
     * Send hard delete request on created AIP verifies its status code.
     * Then send AIP state request and verifies that state of AIP is DELETED, SIP is no more consistent (has been deleted).
     * Also verifies that two XMLs of SIP does not have the same version number.
     * At the end send request for AIP data and verifies that 404 error status is retrieved.
     *
     * @throws Exception
     */
    @Test
    public void getAfterDelete() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}/hard", SIP_ID))
                .andExpect(status().isOk());
        Thread.sleep(2000);
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(false))
                .andExpect(jsonPath("$.state").value("DELETED"))
                .andExpect(jsonPath("$.xmls[0].version", not(equalTo(jsonPath("$.xmls[1].version")))));
        mvc(api)
                .perform(get(BASE + "/{sipId}", SIP_ID))
                .andExpect(status().is(404));
    }

    /**
     * Send hard delete request on created AIP verifies its status code.
     * While deletion request is being processed sends requests for AIP data and state which both returns 423 "LOCKED" state.
     *
     * @throws Exception
     */
    @Test
    public void getDuringDelete() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}/hard", SIP_ID))
                .andExpect(status().isOk());
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isLocked());
        mvc(api)
                .perform(get(BASE + "/{sipId}", SIP_ID))
                .andExpect(status().isLocked());
        Thread.sleep(2000);
    }

    /**
     * Send request for AIP state and verifies that it is in ARCHIVED state and it has two xml versions.
     *
     * @throws Exception
     */
    @Test
    public void aipState() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ARCHIVED"))
                .andExpect(jsonPath("$.xmls[0].version", not(equalTo(jsonPath("$.xmls[1].version")))));
    }

    /**
     * Send request for storage state and verifies response contains number of free bytes and type of storage.
     *
     * @throws Exception
     */
    @Test
    public void getStorageState() throws Exception {
        mvc(api)
                .perform(get(BASE + "/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.free").isNumber())
                .andExpect(jsonPath("$.type", equalTo(StorageType.FILESYSTEM.toString())));
    }

    /**
     * Send request with invalid MD5 and verifies that response contains BAD_REQUEST error status.
     *
     * @throws Exception
     */
    @Test
    public void invalidMD5() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());
        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).param("xmlMD5", "invalidhash"))
                .andExpect(status().is(400));
    }

    /**
     * Send request with invalid UUID and verifies that response contains BAD_REQUEST error status.
     *
     * @throws Exception
     */
    @Test
    public void invalidID() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}/hard", "invalidid"))
                .andExpect(status().is(400));
    }

    private static String toXmlId(String sipId, int version) {
        return String.format("%s_xml_%d", sipId, version);
    }
}
