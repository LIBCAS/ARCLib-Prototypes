package cz.inqool.arclib;

import cz.inqool.arclib.clamAV.ClamSIPAntivirusScanner;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SIPAntivirusScannerTests {

    @Test(expected = IllegalArgumentException.class)
    public void nullFilePathTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        scanner.scan(null);
    }

    @Test(expected = FileNotFoundException.class)
    public void fileNotFoundPathTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        scanner.scan("invalid path");
    }

    @Test()
    public void corruptedFolderTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        List<Path> corruptedFiles = scanner.scan(Paths.get("src/test/resources/testSIP").toAbsolutePath().toString());
        Path corruptedFile = Paths.get("src/test/resources/testSIP/eicar.com").toAbsolutePath();
        assertThat(corruptedFiles, hasSize(1));
        assertThat(corruptedFiles, containsInAnyOrder(corruptedFile));
    }

    @Test()
    public void okFileTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        List<Path> corruptedFiles = scanner.scan(new File("src/test/resources/testSIP/clean.txt").getAbsolutePath());
        assertThat(corruptedFiles, empty());
    }
}
