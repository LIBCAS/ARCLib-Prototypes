package cas.lib.arclib;

import cas.lib.arclib.exception.DroidFormatIdentifierException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface FormatIdentifier {

    Map<String, String> analyze(Path pathToSIP) throws IOException, InterruptedException, DroidFormatIdentifierException;
}
