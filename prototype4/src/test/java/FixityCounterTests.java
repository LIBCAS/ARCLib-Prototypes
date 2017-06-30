import cz.inqool.arclib.Md5FixityCounter;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class FixityCounterTests {

    private Md5FixityCounter fixityCounter;
    private static final String DIGEST = "6f1ed002ab5595859014ebf0951522d9";
    private static final Path PATH_TO_FILE = Paths.get("src/test/resources/sample.txt");

    @Before
    public void setUp() {
        fixityCounter=new Md5FixityCounter();
    }

    @Test
    public void testOK() throws IOException {
        assertThat(fixityCounter.verifyFixity(PATH_TO_FILE,DIGEST),equalTo(true));}

    @Test
    public void testCorrupted() throws IOException{
        String corruptedDigest = DIGEST.substring(1) + "1";
        assertThat(fixityCounter.verifyFixity(PATH_TO_FILE,corruptedDigest),equalTo(false));}

    @Test(expected= FileNotFoundException.class)
    public void testNotFound() throws IOException {
        fixityCounter.computeDigest(Paths.get("invalidpath"));
    }

    @Test(expected= IllegalArgumentException.class)
    public void testNullPath() throws IOException {
        fixityCounter.computeDigest(null);
    }
}
