package cz.cas.lib.arclib;

import cz.cas.lib.arclib.dto.StorageStateDto;
import cz.cas.lib.arclib.storage.FileSystemStorageService;
import cz.cas.lib.arclib.storage.StorageService;
import cz.cas.lib.arclib.storage.StorageType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static cz.cas.lib.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FSStorageServiceTest {

    private static final String CHARSET = "UTF-8";

    private static final String SIP_ID = "SIPtestID";
    private static final String XML1_ID = "SIPtestID_xml_1";
    private static final String XML2_ID = "SIPtestID_xml_2";
    private static final String XML1_CONTENT = "XML1testID";
    private static final String XML2_CONTENT = "XML2testID";


    private static final Path SIP_PATH_DIRS = Paths.get(SIP_ID.substring(0, 2), SIP_ID.substring(2, 4), SIP_ID.substring(4, 6));
    private static final Path SIP_PATH = Paths.get("sip", SIP_PATH_DIRS.toString());
    private static final Path XML_PATH = Paths.get("xml", SIP_PATH_DIRS.toString());

    private static final InputStream SIP_STREAM = new ByteArrayInputStream(SIP_ID.getBytes(StandardCharsets.UTF_8));
    private static final InputStream XML1_STREAM = new ByteArrayInputStream(XML1_ID.getBytes(StandardCharsets.UTF_8));

    private static final StorageService storage = new FileSystemStorageService();

    @BeforeClass
    public static void setUp() throws IOException {
        if (Files.isDirectory(Paths.get("sip")))
            FileUtils.deleteDirectory(new File("sip"));
        if (Files.isDirectory(Paths.get("xml")))
            FileUtils.deleteDirectory(new File("xml"));
        Files.createDirectories(SIP_PATH);
        Files.createDirectories(XML_PATH);
        Files.copy(Paths.get("./src/test/resources/aip/sip"), SIP_PATH.resolve(SIP_ID));
        Files.copy(Paths.get("./src/test/resources/aip/xml1.xml"), XML_PATH.resolve(XML1_ID));
        Files.copy(Paths.get("./src/test/resources/aip/xml2.xml"), XML_PATH.resolve(XML2_ID));
    }

    @Before
    public void beforeTest() throws IOException {
        SIP_STREAM.reset();
        XML1_STREAM.reset();
    }

    @Test
    public void storeAipOK() throws IOException {
        Path xmlP = Paths.get("./xml/SI/Pu/ui/SIPuuid_xml_1");
        Path sipP = Paths.get("./sip/SI/Pu/ui/SIPuuid");

        storage.storeAip(SIP_STREAM, "SIPuuid", XML1_STREAM);

        assertThat(Files.exists(xmlP), equalTo(true));
        assertThat(Files.exists(sipP), equalTo(true));
        assertThat(new String(Files.readAllBytes(sipP), CHARSET), equalTo(SIP_ID));
        assertThat(new String(Files.readAllBytes(xmlP), CHARSET), equalTo(XML1_ID));
    }

    @Test
    public void storeAipFileExists() throws IOException {
        assertThrown(() -> storage.storeAip(SIP_STREAM, SIP_ID, XML1_STREAM)).isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    public void getAipOK() throws IOException {
        List<InputStream> fileRefs = new ArrayList<>();
        try {
            fileRefs = storage.getAip(SIP_ID, 1, 2);
            assertThat(IOUtils.toString(fileRefs.get(0), CHARSET), equalTo(SIP_ID));
            assertThat(IOUtils.toString(fileRefs.get(1), CHARSET), equalTo(XML1_CONTENT));
            assertThat(IOUtils.toString(fileRefs.get(2), CHARSET), equalTo(XML2_CONTENT));
        } finally {
            for (InputStream is : fileRefs) {
                IOUtils.closeQuietly(is);
            }
        }
    }

    @Test
    public void getAipFileDoesNotExist() throws IOException {
        assertThrown(() -> storage.getAip(SIP_ID, 1, 77)).isInstanceOf(FileNotFoundException.class);
        assertThrown(() -> storage.getAip("WRONGSIPID", 1, 2)).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void storeXmlOK() throws IOException {
        storage.storeXml(XML1_STREAM, "sipIdOk",2);
        Path xmlP = Paths.get("xml/si/pI/dO/sipIdOk_xml_2");
        assertThat(Files.exists(xmlP), equalTo(true));
        assertThat(new String(Files.readAllBytes(xmlP), CHARSET), equalTo(XML1_ID));
    }

    @Test
    public void storeXmlFileExists() throws IOException {
        assertThrown(() -> storage.storeXml(XML1_STREAM, SIP_ID,1)).isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    public void getXmlOK() throws IOException {
        InputStream xmlRef = null;
        try {
            xmlRef = storage.getXml(SIP_ID,1);
            assertThat(IOUtils.toString(xmlRef, CHARSET), equalTo(XML1_CONTENT));
        } finally {
            IOUtils.closeQuietly(xmlRef);
        }
    }

    @Test
    public void getXmlSipDoesNotExist() {
        assertThrown(() -> storage.getXml("WRONGSIPID",1)).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void getXmlVersionDoesNotExist() {
        assertThrown(() -> storage.getXml(SIP_ID,99)).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void deleteSipOK() throws IOException {
        storage.storeAip(SIP_STREAM, "deleteTestSIP", XML1_STREAM);
        storage.deleteSip("deleteTestSIP");
        assertThat(Files.notExists(Paths.get("./sip/de/le/te/deleteTestSIP")), equalTo(true));
        assertThat(Files.exists(Paths.get("./xml/de/le/te/deleteTestSIP_xml_1")), equalTo(true));
    }

    @Test
    public void getStorageState() throws IOException {
        StorageStateDto before = storage.getStorageState();
        byte[] b = new byte[1024];
        new Random().nextBytes(b);
        storage.storeXml(new ByteArrayInputStream(b), "STORAGECAPACITYTEST",10);
        StorageStateDto after = storage.getStorageState();
        assertThat(before.getNodes(), empty());
        assertThat(before.getType(), is(StorageType.FILESYSTEM));
        assertThat(before.getCapacity(), equalTo(after.getCapacity()));
        assertThat(before.getFree(), greaterThan(after.getFree()));
        assertThat(before.getUsed(), lessThan(after.getUsed()));
    }
}
