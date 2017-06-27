import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.service.FixityService;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class FixityServiceTests {

    private FixityService service;

    @Before
    public void setUp() {
        service=new FixityService();
    }

    @Test
    public void testArchivedState() throws IOException, NoSuchAlgorithmException {
        assertThat(service.getAipState(Paths.get("src/test/resources/archived.txt")),equalTo(AipState.ARCHIVED));
    }

    @Test
    public void testCorruptedState() throws IOException, NoSuchAlgorithmException {
        assertThat(service.getAipState(Paths.get("src/test/resources/corrupted.txt")),equalTo(AipState.CORRUPTED));
    }
    @Test
    public void testNotFoundState() throws IOException, NoSuchAlgorithmException {
        assertThat(service.getAipState(Paths.get("src/test/resources/notFound.txt")), equalTo(AipState.NOT_FOUND));
    }
}
