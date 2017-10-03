package cz.cas.lib.arclib.storage;

import cz.cas.lib.arclib.dto.AipCreationMd5Info;
import cz.cas.lib.arclib.dto.StorageStateDto;
import cz.cas.lib.arclib.store.Transactional;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.cas.lib.arclib.util.Utils.bytesToHexString;
import static cz.cas.lib.arclib.util.Utils.notNull;

@Service
@Transactional
@Log4j
/**
 * File System implementation of {@link StorageService}. Files are stored to filesystem into <i>sip</i> and <i>xml</i> folders in <i>working directory</i>.
 * <p>Data are distributed into three level folder structure based on their uuid. E.g. sip file with id <i>38a4a26f-67fd-4e4c-8af3-1fd0f26465f6</i> will be stored into sip/38/a4/a2 folder</p>
 * <p>
 * Store files in that way that later it is possible to retrieve:
 * <ul>
 *     <li>initial MD5 checksum of file: checksums are stored during creation to the same directory as file into text file with file name and <i>.MD5</i> suffix</li>
 *     <li>creation time of file: provided by filesystem</li>
 *     <li>info if file is being processed: new empty file with original file name and <i>.LOCK</i> suffix is created when processing starts and deleted when it ends</li>
 *     <li>for SIP its ID and info if is {@link cz.cas.lib.arclib.domain.AipState#DELETED} or {@link cz.cas.lib.arclib.domain.AipState#REMOVED}:
 *     SIP ID is its file name, when SIP is DELETED its files are no longer stored, when its REMOVED new empty file with SIP ID and <i>.REMOVED</i> sufffix is created</li>
 *     <li>for XML its version and ID of SIP: XML file name follows <i>'SIPID'_xml_'XMLVERSION'</i> pattern</li>
 * </ul>
 * <b>For testing purposes, this prototype implementation uses {@link Thread#sleep(long)} in create/delete methods to simulate time-consuming operations.</b>
 */
public class FileSystemStorageService implements StorageService {

    @Override
    public AipCreationMd5Info storeAip(InputStream sip, String sipId, InputStream xml) throws IOException {
        String storageXmlId = String.format("%s_xml_1", sipId);
        return new AipCreationMd5Info(storeFile(sip, sipId), storeFile(xml, storageXmlId));
    }

    @Override
    public List<InputStream> getAip(String sipId, Integer... xmlVersions) throws IOException {
        List<InputStream> refs = new ArrayList<>();
        try {
            refs.add(new FileInputStream(getSipPath(sipId).resolve(sipId).toString()));
            for (int version : xmlVersions) {
                refs.add(new FileInputStream(getXmlPath(sipId).resolve(toXmlId(sipId, version)).toString()));
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
    public String storeXml(InputStream xml, String sipId, int xmlVersion) throws IOException {
        return storeFile(xml, toXmlId(sipId, xmlVersion));
    }

    @Override
    public InputStream getXml(String sipId, int version) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(getXmlPath(sipId).resolve(toXmlId(sipId, version)).toString());
        } catch (Exception ex) {
            IOUtils.closeQuietly(is);
            throw ex;
        }
        return is;
    }

    @Override
    public void deleteSip(String sipId, boolean rollback) throws IOException {
        Path sipPath = getSipPath(sipId);
        if (Files.notExists(sipPath.resolve(String.format("%s.LOCK", sipId))))
            Files.createFile(sipPath.resolve(String.format("%s.LOCK", sipId)));
        Files.deleteIfExists(sipPath.resolve(sipId));
        Files.deleteIfExists(sipPath.resolve(String.format("%s.REMOVED", sipId)));
        Files.deleteIfExists(sipPath.resolve(String.format("%s.MD5", sipId)));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (rollback)
            sipPath.resolve(String.format("%s.LOCK", sipId)).toFile().renameTo(sipPath.resolve(String.format("%s.ROLLBACKED", sipId)).toFile());
        else
            Files.delete(sipPath.resolve(String.format("%s.LOCK", sipId)));
    }

    @Override
    public void deleteXml(String sipId, int version) throws IOException {
        String xmlId = toXmlId(sipId, version);
        Path xmlPath = getXmlPath(sipId);
        if (Files.notExists(xmlPath.resolve(String.format("%s.LOCK", xmlId))))
            Files.createFile(xmlPath.resolve(String.format("%s.LOCK", xmlId)));
        Files.deleteIfExists(xmlPath.resolve(xmlId));
        Files.deleteIfExists(xmlPath.resolve(String.format("%s.MD5", xmlId)));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        xmlPath.resolve(String.format("%s.LOCK", xmlId)).toFile().renameTo(xmlPath.resolve(String.format("%s.ROLLBACKED", xmlId)).toFile());
    }

    @Override
    public void remove(String sipId) throws IOException {
        Path filePath = getSipPath(sipId);
        try {
            Files.createFile(filePath.resolve(String.format("%s.REMOVED", sipId)));
        } catch (FileAlreadyExistsException e) {
        }
    }

    @Override
    public Map<Integer, String> getXmlsMD5(String sipId, List<Integer> xmlVersions) throws IOException {
        Map<Integer, String> checksums = new HashMap<>();
        for (int version : xmlVersions) {
            checksums.put(version, computeMD5(getXmlPath(sipId).resolve(toXmlId(sipId, version))));
        }
        return checksums;
    }

    @Override
    public String getSipMD5(String sipId) throws IOException {
        return computeMD5(getSipPath(sipId).resolve(sipId));
    }

    @Override
    public StorageStateDto getStorageState() {
        File anchor = new File(".");
        return new StorageStateDto(anchor.getTotalSpace(), anchor.getFreeSpace(), true, StorageType.FILESYSTEM);
    }

    private Path getXmlPath(String id) throws IOException {
        return getPath(id, "xml");
    }

    private Path getSipPath(String id) throws IOException {
        return getPath(id, "sip");
    }

    private Path getPath(String id, String fileType) throws IOException {
        Path dirPath = Paths.get(fileType, id.substring(0, 2), id.substring(2, 4), id.substring(4, 6));
        if (Files.notExists(dirPath))
            Files.createDirectories(dirPath);
        return Paths.get(dirPath.toString());
    }

    private String storeFile(InputStream file, String id) throws IOException {
        Path filePath = id.contains("_xml_") ? getXmlPath(id) : getSipPath(id);
        Files.createFile(filePath.resolve(String.format("%s.LOCK", id)));
        Files.copy(file, filePath.resolve(id));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String md5 = computeMD5(filePath.resolve(id));
        Files.copy(new ByteArrayInputStream(md5.getBytes()), filePath.resolve(String.format("%s.MD5", id)));
        Files.delete(filePath.resolve(String.format("%s.LOCK", id)));
        return md5;
    }

    private String computeMD5(Path pathToFile) {
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
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            log.warn(String.format("IO error occurred while computing MD5 of file on path: %s. %s", pathToFile.toString(), e.toString()));
            return null;
        }
    }

    private static String toXmlId(String sipId, int version) {
        return String.format("%s_xml_%d", sipId, version);
    }
}
