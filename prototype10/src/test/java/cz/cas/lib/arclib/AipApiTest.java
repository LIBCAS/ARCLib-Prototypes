package cz.cas.lib.arclib;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.api.AipApi;
import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.helper.ApiTest;
import cz.cas.lib.arclib.helper.DbTest;
import cz.cas.lib.arclib.storage.StorageType;
import cz.cas.lib.arclib.store.AipSipStore;
import cz.cas.lib.arclib.store.AipXmlStore;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
    private static final String XML1_HASH = "F09E5F27526A0ED7EC5B2D9D5C0B53CF";

    private static final String XML2_ID = "XML2testID";
    private static final String XML2_HASH = "D5B6402517014CF00C223D6A785A4230";

    private static final Path SIP_PATH_DIRS = Paths.get(SIP_ID.substring(0, 2), SIP_ID.substring(2, 4), SIP_ID.substring(4, 6));
    private static final Path SIP_PATH = Paths.get("sip").resolve(SIP_PATH_DIRS);
    private static final Path XML_PATH = Paths.get("xml").resolve(SIP_PATH_DIRS);

    private static final String BASE = "/api/storage";
    private static final Path SIP_SOURCE_PATH = Paths.get("..", "KPW01169310.ZIP");

    /**
     * Recursively deletes old test directories, create new and copies one SIP and two XMLs to them.
     * @throws IOException
     */
    @BeforeClass
    public static void setUp() throws IOException {
        if (Files.isDirectory(Paths.get("sip")))
            FileUtils.deleteDirectory(new File("sip"));
        if (Files.isDirectory(Paths.get("xml")))
            FileUtils.deleteDirectory(new File("xml"));
        Files.createDirectories(SIP_PATH);
        Files.createDirectories(XML_PATH);
        Files.copy(Paths.get(SIP_SOURCE_PATH.toString()), SIP_PATH.resolve(SIP_ID));
        Files.copy(Paths.get("./src/test/resources/aip/xml1.xml"), XML_PATH.resolve(toXmlId(SIP_ID, 1)));
        Files.copy(Paths.get("./src/test/resources/aip/xml2.xml"), XML_PATH.resolve(toXmlId(SIP_ID, 2)));
    }

    /**
     * Creates new database records before every test
     */
    @Before
    public void before() {
        xmlStore.setEntityManager(getEm());
        xmlStore.setQueryFactory(new JPAQueryFactory(getEm()));

        sipStore.setEntityManager(getEm());
        sipStore.setQueryFactory(new JPAQueryFactory(getEm()));

        AipSip sip = new AipSip(SIP_ID, SIP_HASH, AipState.ARCHIVED);
        sipStore.save(sip);
        xmlStore.save(new AipXml(XML1_ID, XML1_HASH, sip, 1, false));
        xmlStore.save(new AipXml(XML2_ID, XML2_HASH, sip, 2, false));
    }

    /**
     * Clear database records after each test
     * @throws SQLException
     */
    @After
    public void after() throws SQLException {
        clearDatabase();
    }

    /**
     * Send request for AIP data and verifies that ZIP file containing one ZIP (SIP) and latest AIP XML is retrieved.
     *
     * @throws Exception
     */
    @Test
    public void getTest() throws Exception {
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
     *
     * @throws Exception
     */
    @Test
    public void saveTest() throws Exception {
        MockMultipartFile sipFile = new MockMultipartFile(
                "sip", "sip", "text/plain", Files.readAllBytes(SIP_SOURCE_PATH));
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML1_ID.getBytes());
        String id = mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/store").file(sipFile).file(xmlFile).param("sipMD5", SIP_HASH).param("xmlMD5", XML1_HASH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(id, not(isEmptyOrNullString()));
    }

    /**
     * Send AIP creation request with AIP data where MD5 param does not match MD5 of a file and verifies that response contains error code.
     *
     * @throws Exception
     */
    @Test
    public void saveMD5ChangedTest() throws Exception {
        MockMultipartFile sipFile = new MockMultipartFile(
                "sip", "sip", "text/plain", Files.readAllBytes(SIP_SOURCE_PATH));
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML1_ID.getBytes());

        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/store").file(sipFile).file(xmlFile).param("sipMD5", XML1_HASH).param("xmlMD5", XML1_HASH))
                .andExpect(status().is(500));
    }

    /**
     * Send request for XML update and verifies that response status is OK.
     *
     * @throws Exception
     */
    @Test
    public void updateXmlTest() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());
        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).param("xmlMD5", XML2_HASH))
                .andExpect(status().isOk());
    }

    /**
     * Send request for XML update where MD5 param does not match MD5 of a file and verifies that response contains error code.
     *
     * @throws Exception
     */
    @Test
    public void updateXmlMD5ChangedTest() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());

        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).param("xmlMD5", SIP_HASH))
                .andExpect(status().is(500));
    }

    /**
     * Send AIP remove (soft delete) request and verifies that 200 status code is retrieved.
     *
     * @throws Exception
     */
    @Test
    public void removeTest() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}", SIP_ID))
                .andExpect(status().isOk());
    }

    /**
     * Send hard delete request on created AIP verifies its status code.
     * Then send AIP state request and verifies that state of AIP is DELETED, SIP is no more consistent (has been deleted).
     * Also verifies that two XMLs of SIP does not have the same version number.
     * At the end send request for AIP data and verifies that NOT_FOUND error status is retrieved.
     *
     * @throws Exception
     */
    @Test
    public void deleteThenGetTest() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}/hard", SIP_ID))
                .andExpect(status().isOk());
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
     * Send request for AIP state and verifies that it is in ARCHIVED state and it has two xml versions.
     *
     * @throws Exception
     */
    @Test
    public void aipStateTest() throws Exception {
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
