package cz.inqool.arclib.clamAV;

import cz.inqool.arclib.SIPAntivirusScanner;
import cz.inqool.arclib.SIPAntivirusScannerException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.inqool.arclib.Util.Utils.notNull;

@Service
public class ClamSIPAntivirusScanner implements SIPAntivirusScanner {

    private static final String CMD = "clamscan";

    /**
     * Scans SIP package for viruses.
     * <i>clamscan</i> command has to be executable from commandline
     *
     * @param pathToSIP absoulte path to SIP
     * @return list with paths to corrupted files if threat was detected, empty list otherwise
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InterruptedException
     * @throws SIPAntivirusScannerException if error occurs during the antivirus scan process
     */
    @Override
    public List<Path> scan(String pathToSIP) throws IOException, InterruptedException, SIPAntivirusScannerException {
        notNull(pathToSIP, () -> {
            throw new IllegalArgumentException("null path to SIP package");
        });
        if (!new File(pathToSIP).exists())
            throw new FileNotFoundException("no file/folder found at: " + pathToSIP);
        BufferedReader br;
        StringBuilder sb;
        String line;
        ProcessBuilder pb = new ProcessBuilder(CMD, "-r", pathToSIP);
        Process p = pb.start();
        List<Path> corruptedFiles = new ArrayList<>();
        switch (p.waitFor()) {
            case 0:
                return corruptedFiles;
            case 1:
                br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = br.readLine();
                Matcher matcher;
                Pattern pattern = Pattern.compile("(.+): .+ FOUND");
                while (line != null) {
                    matcher = pattern.matcher(line);
                    if (matcher.find())
                        corruptedFiles.add(Paths.get(matcher.group(1)));
                    line = br.readLine();
                }
                return corruptedFiles;
            default:
                br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                sb = new StringBuilder();
                line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                throw new SIPAntivirusScannerException(sb.toString());
        }
    }

    /**
     * Moves files to quarantine. There must be CLAMAV environment variable pointing to CLAMAV directory.
     *
     * @param infectedFiles
     */
    @Override
    public void moveToQuarantine(List<Path> infectedFiles) throws IOException {
        infectedFiles.stream().forEach(
                path -> {
                    try {
                        Files.move(path, Paths.get(System.getenv("CLAMAV"), "quarantine").resolve(path.getFileName()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}

