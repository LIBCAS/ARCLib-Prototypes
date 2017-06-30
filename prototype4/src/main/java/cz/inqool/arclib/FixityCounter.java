package cz.inqool.arclib;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static cz.inqool.arclib.utils.Utils.bytesToHexString;
import static cz.inqool.arclib.utils.Utils.notNull;

public abstract class FixityCounter {

    public abstract byte[] computeDigest(Path pathToFile) throws IOException;

    public boolean verifyFixity(Path pathToFile, String expectedDigest) throws IOException {
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        notNull(expectedDigest, () -> {
            throw new IllegalArgumentException();
        });
        return expectedDigest.equals(bytesToHexString(computeDigest(pathToFile)));
    }

    public boolean verifyFixity(Path pathToFile, byte[] expectedDigest) throws IOException {
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        notNull(expectedDigest, () -> {
            throw new IllegalArgumentException();
        });
        return Arrays.equals(expectedDigest, computeDigest(pathToFile));
    }
}
