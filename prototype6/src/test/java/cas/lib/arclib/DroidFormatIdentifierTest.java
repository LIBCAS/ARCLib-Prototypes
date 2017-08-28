package cas.lib.arclib;

import cas.lib.arclib.droid.DroidFormatIdentifier;
import cas.lib.arclib.exception.DroidFormatIdentifierException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DroidFormatIdentifierTest {

    private static final Path sipPath = Paths.get("../KPW01169310");
    private DroidFormatIdentifier formatIdentifier;

    @Before
    public void setUp() throws IOException {
        formatIdentifier = new DroidFormatIdentifier();
        formatIdentifier.setPathToSignatureFile("src/main/resources/DROID_SignatureFile_V88.xml");

    }

    @Test()
    public void analyzeSuccessTest() throws InterruptedException, IOException, DroidFormatIdentifierException {
        Map<String, String> analyze = formatIdentifier.analyze(sipPath);
        System.out.println(analyze);
    }
}
