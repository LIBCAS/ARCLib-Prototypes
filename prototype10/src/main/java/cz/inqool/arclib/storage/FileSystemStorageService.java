package cz.inqool.arclib.storage;

import cz.inqool.arclib.dto.StorageStateDto;
import cz.inqool.uas.store.Transactional;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.inqool.uas.util.Utils.bytesToHexString;
import static cz.inqool.uas.util.Utils.notNull;

@Service
@Transactional
/**
 * File System implementation of {@link StorageService}. Files are stored to filesystem into <i>sip</i> and <i>xml</i> folders in <i>working directory</i>.
 * <p>Data are distributed into three level folder structure based on their uuid. E.g. sip file with id <i>38a4a26f-67fd-4e4c-8af3-1fd0f26465f6</i> will be stored into sip/38/a4/a2 folder</p>
 */
public class FileSystemStorageService implements StorageService {

    @Override
    public Map<String, String> storeAip(InputStream sip, String sipId, InputStream xml, String xmlId) throws IOException {
        Map<String, String> checksums = new HashMap<>();
        checksums.put(sipId, storeSipFile(sip, sipId));
        checksums.put(xmlId, storeXmlFile(xml, xmlId));
        return checksums;
    }

    @Override
    public List<InputStream> getAip(String sipId, String... xmlIds) throws IOException {
        List<InputStream> refs = new ArrayList<>();
        try {
            refs.add(new FileInputStream(getSipFilePath(sipId).toString()));

            for (String xmlId : xmlIds) {
                refs.add(new FileInputStream(getXmlFilePath(xmlId).toString()));
            }
        } catch (Exception ex) {
            for (InputStream stream : refs) {
                IOUtils.closeQuietly(stream);
            }
            throw ex;
        }
        return refs;
    }

    @Override
    public String storeXml(InputStream xml, String xmlId) throws IOException {
        return storeXmlFile(xml, xmlId);
    }

    @Override
    public InputStream getXml(String xmlId) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(getXmlFilePath(xmlId).toString());
        } catch (Exception ex) {
            IOUtils.closeQuietly(is);
            throw ex;
        }
        return is;
    }

    @Override
    public void deleteSip(String sipId) throws IOException {
        Files.delete(getSipFilePath(sipId));
    }

    @Override
    public Map<String, String> getMD5(String sipId, String... xmlIds) throws IOException {
        Map<String, String> checksums = new HashMap<>();
        checksums.put(sipId, computeMD5(getSipFilePath(sipId)));
        for (String xmlId : xmlIds) {
            checksums.put(sipId, computeMD5(getXmlFilePath(xmlId)));
        }
        return checksums;
    }

    @Override
    public StorageStateDto getStorageState() {
        File anchor = new File(".");
        return new StorageStateDto(anchor.getTotalSpace(), anchor.getFreeSpace(), true, StorageType.FILESYSTEM);
    }

    private Path getXmlFilePath(String uuid) throws IOException {
        Path dirPath = Paths.get("xml", uuid.substring(0, 2), uuid.substring(2, 4), uuid.substring(4, 6));
        Files.createDirectories(dirPath);
        return Paths.get(dirPath.toString(), uuid);
    }

    private Path getSipFilePath(String uuid) throws IOException {
        Path dirPath = Paths.get("sip", uuid.substring(0, 2), uuid.substring(2, 4), uuid.substring(4, 6));
        Files.createDirectories(dirPath);
        return Paths.get(dirPath.toString(), uuid);
    }

    private String storeSipFile(InputStream file, String id) throws IOException {
        Files.copy(file, getSipFilePath(id));
        return computeMD5(getSipFilePath(id));
    }

    private String storeXmlFile(InputStream file, String id) throws IOException {
        Files.copy(file, getXmlFilePath(id));
        return computeMD5(getXmlFilePath(id));
    }

    private String computeMD5(Path pathToFile) throws IOException {
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
            return bytesToHexString(complete.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
