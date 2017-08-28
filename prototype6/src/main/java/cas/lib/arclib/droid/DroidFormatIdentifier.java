package cas.lib.arclib.droid;

import cas.lib.arclib.FormatIdentifier;
import cas.lib.arclib.exception.DroidFormatIdentifierException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static cas.lib.arclib.Util.Utils.notNull;

@Service
@Slf4j
public class DroidFormatIdentifier implements FormatIdentifier {

    private static final String CMD = "C:\\Program Files\\Droid\\droid-binary-6.4-bin\\droid.bat";
    protected String pathToSignatureFile;


    @Override
    public Map<String, String> analyze(Path pathToSIP) throws IOException, InterruptedException, DroidFormatIdentifierException {
        log.info("analyzing SIP at path: " + pathToSIP);
        notNull(pathToSIP, () -> {
            throw new IllegalArgumentException("null path to SIP package");
        });
        if (!new File(pathToSIP.toString()).exists())
            throw new FileNotFoundException("no file/folder found at: " + pathToSIP);
        BufferedReader br;
        StringBuilder sb;
        String line;
        log.info("running '" + CMD + " -r " + pathToSIP + "' process");
        ProcessBuilder pb = new ProcessBuilder(CMD,
                "-R",
                "-Nr",
                "\"" + pathToSIP.toAbsolutePath().toString() + "\"",
                "-Ns",
                "\"" + Paths.get(pathToSignatureFile).toAbsolutePath().toString() + "\"");
        Process p = pb.start();
        Map<String, String> filesToFormats = new HashMap<>();
        switch (p.waitFor()) {
            case 0:
                br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = br.readLine();

                String pathString = pathToSIP.toAbsolutePath().normalize().toString();

                while (line != null) {
                    if (line.startsWith(pathString)) {

                        String absoluteFilePath = line.substring(0, line.indexOf(",")).trim();
                        String relativeFilePath = absoluteFilePath.substring(pathString.length() + 1);
                        String format = line.substring(line.lastIndexOf(",") + 1).trim();

                        filesToFormats.put(relativeFilePath, format);
                    }
                    line = br.readLine();
                }
                return filesToFormats;
            default:
                br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                sb = new StringBuilder();
                line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                throw new DroidFormatIdentifierException(sb.toString());
        }
    }

    @Inject
    public void setPathToSignatureFile(@Value("${arclib.pathToSignatureFile}") String pathToSignatureFile) {
        this.pathToSignatureFile = pathToSignatureFile;
    }
}
