package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.AipState;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Service
public class FixityService {

    /**
     * Computes MD5 digest of specified aip, compares this digest with digest attached to the aip and returns state of the aip package.
     * Digest attached to aip has to be located in the same folder with the same name with extension <i>.digest</i>
     * @param pathToAip  path to aip package: aip package is for simplicity of the prototype treated as locally stored file
     * @return
     *  AipState.ARCHIVED if the aip is stored and its md5 digest matches its assigned digest
     *  AipState.CORRUPTED if the aip is stored but its md5 digest does not match its assigned digest
     *  AipState.NOT_FOUND if the aip is not stored in the specified location
     */
    public AipState getAipState(Path pathToAip) throws IOException, NoSuchAlgorithmException {
        if(!Files.exists(pathToAip))
            return AipState.NOT_FOUND;
        Path pathToAipDigest = Paths.get(pathToAip.toAbsolutePath().toString()+".digest");
        if(Files.readAllLines(pathToAipDigest).get(0).equals(computeMD5(pathToAip)))
            return AipState.ARCHIVED;

        return AipState.CORRUPTED;
    }

    private String computeMD5(Path pathToAip) throws NoSuchAlgorithmException, IOException {
        try(
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pathToAip.toAbsolutePath().toString()))){
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = bis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            final StringBuilder builder = new StringBuilder();
            for(byte b : complete.digest()) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
    }
}
