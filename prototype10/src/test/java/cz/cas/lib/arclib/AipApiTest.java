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
    private static final String SIP_FILE_NAME = "KPW01169310.ZIP";
    private static final String SIP_HASH = "CB30ACE944A440F77D6F99040D2DE1F2";
    private static final Path SIP_PATH = Paths.get("sip", SIP_ID.substring(0, 2), SIP_ID.substring(2, 4), SIP_ID.substring(4, 6));

    private static final String XML1_ID = "3139b6fa-4a73-4e93-9ed9-9cdcc4523d94";
    private static final String XML1_FILE_NAME = "xml1.xml";
    private static final String XML1_HASH = "3109a2b5b54b881f234fd424b80869ef";
    private static final Path XML1_PATH = Paths.get("xml", XML1_ID.substring(0, 2), XML1_ID.substring(2, 4), XML1_ID.substring(4, 6));

    private static final String XML2_ID = "3bdfc2e3-51e3-4b88-a9f6-6dc301673ac1";
    private static final String XML2_FILE_NAME = "xml2.xml";
    private static final String XML2_HASH = "f9408dbe836264462904651820d4162f";
    private static final Path XML2_PATH = Paths.get("xml", XML2_ID.substring(0, 2), XML2_ID.substring(2, 4), XML2_ID.substring(4, 6));

    private static final String BASE = "/api/storage";
    private static final Path SIP_SOURCE_PATH = Paths.get("..", SIP_FILE_NAME);

    @BeforeClass
    public static void setUp() throws IOException {
        if (Files.isDirectory(Paths.get("sip")))
            FileUtils.deleteDirectory(new File("sip"));
        if (Files.isDirectory(Paths.get("xml")))
            FileUtils.deleteDirectory(new File("xml"));
        Files.createDirectories(SIP_PATH);
        Files.createDirectories(XML1_PATH);
        Files.createDirectories(XML2_PATH);
        Files.copy(Paths.get(SIP_SOURCE_PATH.toString()), SIP_PATH.resolve(SIP_ID));
        Files.copy(Paths.get("./src/test/resources/aip/" + XML1_FILE_NAME), XML1_PATH.resolve(XML1_ID));
        Files.copy(Paths.get("./src/test/resources/aip/" + XML2_FILE_NAME), XML2_PATH.resolve(XML2_ID));
    }

    @Before
    public void before() {
        xmlStore.setEntityManager(getEm());
        xmlStore.setQueryFactory(new JPAQueryFactory(getEm()));

        sipStore.setEntityManager(getEm());
        sipStore.setQueryFactory(new JPAQueryFactory(getEm()));

        AipSip sip = new AipSip(SIP_ID, SIP_FILE_NAME, SIP_HASH, AipState.ARCHIVED);
        sipStore.save(sip);
        xmlStore.save(new AipXml(XML1_ID, XML1_FILE_NAME, XML1_HASH, sip, 1, false));
        xmlStore.save(new AipXml(XML2_ID, XML2_FILE_NAME, XML2_HASH, sip, 2, false));
    }

    @After
    public void after() throws SQLException {
        clearDatabase();
    }

    /**
     * Send request for AIP data and verifies that ZIP file containing one ZIP (sip) and two XMLs (aip xmls) is retrieved.
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
        assertThat(packedFiles, containsInAnyOrder((SIP_ID + "_" + SIP_FILE_NAME), (XML1_ID + "_" + XML1_FILE_NAME), (XML2_ID + "_" + XML2_FILE_NAME)));
    }

    /**
     * Send AIP creation request with AIP data (sip & xml) and verifies that response contains names of stored files and their ids which are not the same.
     *
     * @throws Exception
     */
    @Test
    public void saveTest() throws Exception {
        MockMultipartFile sipFile = new MockMultipartFile(
                "sip", "sip", "text/plain", Files.readAllBytes(SIP_SOURCE_PATH));
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML1_ID.getBytes());

        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/store").file(sipFile).file(xmlFile).param("sipMD5", SIP_HASH).param("xmlMD5", XML1_HASH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("sip"))
                .andExpect(jsonPath("$[1].name").value("xml"))
                .andExpect(jsonPath("$[0].id", not(equalTo(jsonPath("$[1].id")))));
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
     * Send request for XML update and verifies that response contains created file name.
     *
     * @throws Exception
     */
    @Test
    public void updateXmlTest() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());

        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).param("xmlMD5", XML2_HASH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("xml"));
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
                .andExpect(jsonPath("$.name").value(SIP_FILE_NAME))
                .andExpect(jsonPath("$.consistent").value(false))
                .andExpect(jsonPath("$.state").value("DELETED"))
                .andExpect(jsonPath("$.xmls[0].version", not(equalTo(jsonPath("$.xmls[1].version")))));
        mvc(api)
                .perform(get(BASE + "/{sipId}", SIP_ID))
                .andExpect(status().is(404));
    }

    /**
     * Send request for AIP state and verifies that it is in ARCHIVED state and it has two xml versions.
     * @throws Exception
     */
    @Test
    public void aipStateTest() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(SIP_FILE_NAME))
                .andExpect(jsonPath("$.state").value("ARCHIVED"))
                .andExpect(jsonPath("$.xmls[0].version", not(equalTo(jsonPath("$.xmls[1].version")))));
    }

    /**
     * Send request for storage state and verifies response contains number of free bytes and type of storage.
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
     * @throws Exception
     */
    @Test
    public void invalidID() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}/hard", "invalidid"))
                .andExpect(status().is(400));
    }
}
