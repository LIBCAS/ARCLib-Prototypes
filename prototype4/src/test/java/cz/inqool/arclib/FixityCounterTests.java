package cz.inqool.arclib;

import cz.inqool.arclib.fixity.Md5FixityCounter;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


public class FixityCounterTests {

    private Md5FixityCounter fixityCounter;
    private static final String DIGEST = "6f1ed002ab5595859014ebf0951522d9";
    private static final Path PATH_TO_FILE = Paths.get("src/test/resources/sample.txt");

    @Before
    public void setUp() {
        fixityCounter = new Md5FixityCounter();
    }

    @Test
    public void testOK() throws IOException {
        assertThat(fixityCounter.verifyFixity(PATH_TO_FILE, DIGEST), equalTo(true));
    }

    @Test
    public void testCorrupted() throws IOException {
        String corruptedDigest = DIGEST.substring(1) + "1";
        assertThat(fixityCounter.verifyFixity(PATH_TO_FILE, corruptedDigest), equalTo(false));
    }

    @Test
    public void testNotFound() throws IOException {
        assertThrown(() -> fixityCounter.computeDigest(Paths.get("invalidpath"))).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void testNullPath() throws IOException {
        assertThrown(() -> fixityCounter.computeDigest(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
