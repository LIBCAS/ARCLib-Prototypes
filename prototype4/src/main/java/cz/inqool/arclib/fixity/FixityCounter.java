package cz.inqool.arclib.fixity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static cz.inqool.arclib.utils.Utils.bytesToHexString;
import static cz.inqool.arclib.utils.Utils.notNull;

public abstract class FixityCounter {

    /**
     * Abstract method to compute digest from a file. The type of digest depends on subclass implementation. Eg. MD5, CRC32, SHA-512 etc.
     *
     * @param pathToFile Path to file which digest has to be computed.
     * @return byte array with computed digest
     * @throws IOException
     */
    public abstract byte[] computeDigest(Path pathToFile) throws IOException;

    /**
     * Computes digest for specified file and compares it with provided digest. The type of computed digest depends on subclass instance.
     * @param pathToFile Path to file which digest has to be computed.
     * @param expectedDigest Digest provided for comparison.
     * @return true if digests matches, false otherwise
     * @throws IOException
     */
    public boolean verifyFixity(Path pathToFile, String expectedDigest) throws IOException {
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        notNull(expectedDigest, () -> {
            throw new IllegalArgumentException();
        });
        return expectedDigest.equals(bytesToHexString(computeDigest(pathToFile)));
    }

    /**
     * Computes digest for specified file and compares it with provided digest. The type of computed digest depends on subclass instance.
     * @param pathToFile Path to file which digest has to be computed.
     * @param expectedDigest Digest provided for comparison.
     * @return true if digests matches, false otherwise
     * @throws IOException
     */
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
