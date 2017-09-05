package cz.inqool.arclib;

import cz.inqool.arclib.dto.StorageStateDto;
import cz.inqool.arclib.storage.FileSystemStorageService;
import cz.inqool.arclib.storage.StorageService;
import cz.inqool.arclib.storage.StorageType;
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

import static cz.inqool.arclib.helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FSStorageServiceTest {

    private static final String CHARSET = "UTF-8";

    private static final String SIP_ID = "SIPtestID";
    private static final String XML1_ID = "XML1testID";
    private static final String XML2_ID = "XML2testID";

    private static final Path SIP_PATH = Paths.get("sip", SIP_ID.substring(0, 2), SIP_ID.substring(2, 4), SIP_ID.substring(4, 6));
    private static final Path XML1_PATH = Paths.get("xml", XML1_ID.substring(0, 2), XML1_ID.substring(2, 4), XML1_ID.substring(4, 6));
    private static final Path XML2_PATH = Paths.get("xml", XML2_ID.substring(0, 2), XML2_ID.substring(2, 4), XML2_ID.substring(4, 6));

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
        Files.createDirectories(XML1_PATH);
        Files.createDirectories(XML2_PATH);
        Files.copy(Paths.get("./src/test/resources/aip/sip"), SIP_PATH.resolve(SIP_ID));
        Files.copy(Paths.get("./src/test/resources/aip/xml1.xml"), XML1_PATH.resolve(XML1_ID));
        Files.copy(Paths.get("./src/test/resources/aip/xml2.xml"), XML2_PATH.resolve(XML2_ID));
    }

    @Before
    public void beforeTest() throws IOException {
        SIP_STREAM.reset();
        XML1_STREAM.reset();
    }

    @Test
    public void storeAipOK() throws IOException {
        Path xmlP = Paths.get("./xml/XM/Lu/ui/XMLuuid");
        Path sipP = Paths.get("./sip/SI/Pu/ui/SIPuuid");

        storage.storeAip(SIP_STREAM, "SIPuuid", XML1_STREAM, "XMLuuid");

        assertThat(Files.exists(sipP), equalTo(true));
        assertThat(Files.exists(xmlP), equalTo(true));
        assertThat(new String(Files.readAllBytes(sipP), CHARSET), equalTo(SIP_ID));
        assertThat(new String(Files.readAllBytes(xmlP), CHARSET), equalTo(XML1_ID));
    }

    @Test
    public void storeAipFileExists() throws IOException {
        assertThrown(() -> storage.storeAip(SIP_STREAM, SIP_ID, XML1_STREAM, XML1_ID)).isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    public void getAipOK() throws IOException {
        List<InputStream> fileRefs = new ArrayList<>();
        try {
            fileRefs = storage.getAip(SIP_ID, XML1_ID, XML2_ID);
            assertThat(IOUtils.toString(fileRefs.get(0), CHARSET), equalTo(SIP_ID));
            assertThat(IOUtils.toString(fileRefs.get(1), CHARSET), equalTo(XML1_ID));
            assertThat(IOUtils.toString(fileRefs.get(2), CHARSET), equalTo(XML2_ID));
        } finally {
            for (InputStream is : fileRefs) {
                IOUtils.closeQuietly(is);
            }
        }
    }

    @Test
    public void getAipFileDoesNotExist() throws IOException {
        assertThrown(() -> storage.getAip(SIP_ID, XML1_ID, "WRONGXMLID")).isInstanceOf(FileNotFoundException.class);
        assertThrown(() -> storage.getAip("WRONGSIPID", XML1_ID, XML1_ID)).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void storeXmlOK() throws IOException {
        storage.storeXml(XML1_STREAM, "newXmlId");
        Path xmlP = Paths.get("xml/ne/wX/ml/newXmlId");
        assertThat(Files.exists(xmlP), equalTo(true));
        assertThat(new String(Files.readAllBytes(xmlP), CHARSET), equalTo(XML1_ID));
    }

    @Test
    public void storeXmlFileExists() throws IOException {
        assertThrown(() -> storage.storeXml(XML1_STREAM, XML1_ID)).isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    public void getXmlOK() throws IOException {
        InputStream xmlRef = null;
        try {
            xmlRef = storage.getXml(XML1_ID);
            assertThat(IOUtils.toString(xmlRef, CHARSET), equalTo(XML1_ID));
        } finally {
            IOUtils.closeQuietly(xmlRef);
        }
    }

    @Test
    public void getXmlFileDoesNotExist() {
        assertThrown(() -> storage.getXml("WRONGXMLID")).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void deleteSipOK() throws IOException {
        storage.storeAip(SIP_STREAM, "deleteTestSIP", XML1_STREAM, "deleteTestXml");
        storage.delete("deleteTestSIP");
        assertThat(Files.notExists(Paths.get("./sip/de/le/te/deleteTestSIP")), equalTo(true));
        assertThat(Files.exists(Paths.get("./xml/de/le/te/deleteTestXML")), equalTo(true));
    }

    @Test
    public void getStorageState() throws IOException {
        StorageStateDto before = storage.getStorageState();
        byte[] b = new byte[1024];
        new Random().nextBytes(b);
        storage.storeXml(new ByteArrayInputStream(b), "STORAGECAPACITYTEST");
        StorageStateDto after = storage.getStorageState();
        assertThat(before.getNodes(), empty());
        assertThat(before.getType(), is(StorageType.FILESYSTEM));
        assertThat(before.getCapacity(), equalTo(after.getCapacity()));
        assertThat(before.getFree(), greaterThan(after.getFree()));
        assertThat(before.getUsed(), lessThan(after.getUsed()));
    }

}
