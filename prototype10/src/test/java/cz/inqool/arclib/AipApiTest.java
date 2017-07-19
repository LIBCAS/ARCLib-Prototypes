package cz.inqool.arclib;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.arclib.api.AipApi;
import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.helper.ApiTest;
import cz.inqool.arclib.helper.DbTest;
import cz.inqool.arclib.storage.StorageType;
import cz.inqool.arclib.store.AipSipStore;
import cz.inqool.arclib.store.AipXmlStore;
import org.apache.commons.io.FileUtils;
import org.junit.*;
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

    private static final String SIP_ID = "SIPtestID";
    private static final String SIP_FILE_NAME = "sip";
    private static final String SIP_HASH = "101B295A91F771D96E1987FF501B034C";
    private static final Path SIP_PATH = Paths.get("sip", SIP_ID.substring(0, 2), SIP_ID.substring(2, 4), SIP_ID.substring(4, 6));


    private static final String XML1_ID = "XML1testID";
    private static final String XML1_FILE_NAME = "xml1";
    private static final String XML1_HASH = "F09E5F27526A0ED7EC5B2D9D5C0B53CF";
    private static final Path XML1_PATH = Paths.get("xml", XML1_ID.substring(0, 2), XML1_ID.substring(2, 4), XML1_ID.substring(4, 6));


    private static final String XML2_ID = "XML2testID";
    private static final String XML2_FILE_NAME = "xml2";
    private static final String XML2_HASH = "D5B6402517014CF00C223D6A785A4230";
    private static final Path XML2_PATH = Paths.get("xml", XML2_ID.substring(0, 2), XML2_ID.substring(2, 4), XML2_ID.substring(4, 6));

    private static final String BASE = "/api/storage";

    @BeforeClass
    public static void setUp() throws IOException {
        Files.createDirectories(SIP_PATH);
        Files.createDirectories(XML1_PATH);
        Files.createDirectories(XML2_PATH);
        Files.copy(Paths.get("./src/test/resources/aip/" + SIP_FILE_NAME), SIP_PATH.resolve(SIP_ID));
        Files.copy(Paths.get("./src/test/resources/aip/" + XML1_FILE_NAME), XML1_PATH.resolve(XML1_ID));
        Files.copy(Paths.get("./src/test/resources/aip/" + XML2_FILE_NAME), XML2_PATH.resolve(XML2_ID));
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        FileUtils.deleteDirectory(new File("sip"));
        FileUtils.deleteDirectory(new File("xml"));
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

    @Test
    public void saveTest() throws Exception {
        MockMultipartFile sipFile = new MockMultipartFile(
                "sip", "sip", "text/plain", SIP_ID.getBytes());
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML1_ID.getBytes());
        MockMultipartFile metaFile = new MockMultipartFile(
                "meta", "meta", "text/plain", (SIP_HASH + System.lineSeparator() + XML1_HASH).getBytes());

        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/store").file(sipFile).file(xmlFile).file(metaFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("sip"))
                .andExpect(jsonPath("$[0].consistent").value(true))
                .andExpect(jsonPath("$[1].name").value("xml"))
                .andExpect(jsonPath("$[1].consistent").value(true))
                .andExpect(jsonPath("$[0].id", not(equalTo(jsonPath("$[1].id")))));
    }

    @Test
    public void updateXmlTest() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile(
                "xml", "xml", "text/plain", XML2_ID.getBytes());
        MockMultipartFile metaFile = new MockMultipartFile(
                "meta", "meta", "text/plain", XML2_HASH.getBytes());

        mvc(api)
                .perform(MockMvcRequestBuilders.fileUpload(BASE + "/{sipId}/update", SIP_ID).file(xmlFile).file(metaFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("xml"))
                .andExpect(jsonPath("$.consistent").value(true));
    }

    @Test
    public void removeTest() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}", SIP_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteTest() throws Exception {
        mvc(api)
                .perform(delete(BASE + "/{sipId}/hard", SIP_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void aipStateTest() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{sipId}/state", SIP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + AipState.ARCHIVED.toString() + "\""));
    }

    @Test
    public void getStorageState() throws Exception {
        mvc(api)
                .perform(get(BASE + "/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.free").isNumber())
                .andExpect(jsonPath("$.type", equalTo(StorageType.FILESYSTEM.toString())));
    }

}
