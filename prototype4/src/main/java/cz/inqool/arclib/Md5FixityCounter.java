package cz.inqool.arclib;

import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static cz.inqool.arclib.utils.Utils.notNull;

@Service
public class Md5FixityCounter extends FixityCounter {

    @Override
    public byte[] computeDigest(Path pathToFile) throws IOException {
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pathToFile.toAbsolutePath().toString()))) {
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = bis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            return complete.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
