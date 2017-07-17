package cz.inqool.arclib.storage;

import cz.inqool.arclib.dto.StorageStateDto;
import cz.inqool.uas.store.Transactional;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
/**
 * File System implementation of {@link StorageService}. Files are stored to filesystem into <i>sip</i> and <i>xml</i> folders in <i>working directory</i>.
 * <p>Data are distributed into three level folder structure based on their uuid. E.g. sip file with id <i>38a4a26f-67fd-4e4c-8af3-1fd0f26465f6</i> will be stored into sip/38/a4/a2 folder</p>
 */
public class FileSystemStorageService implements StorageService {

    @Override
    public void storeAip(InputStream sip, String sipId, InputStream xml, String xmlId) throws IOException {
        storeSipFile(sip, sipId);
        storeXmlFile(xml, xmlId);
    }

    @Override
    public List<InputStream> getAip(String sipId, String... xmlIds) throws IOException {
        List<InputStream> refs = new ArrayList<>();
        refs.add(new FileInputStream(getSipFilePath(sipId).toString()));
        for (String xmlId : xmlIds) {
            refs.add(new FileInputStream(getXmlFilePath(xmlId).toString()));
        }
        return refs;
    }

    @Override
    public void storeXml(InputStream xml, String xmlId) throws IOException {
        storeXmlFile(xml, xmlId);
    }

    @Override
    public InputStream getXml(String xmlId) throws IOException {
        return new FileInputStream(getXmlFilePath(xmlId).toString());
    }

    @Override
    public void deleteSip(String sipId) throws IOException {
        Files.delete(getSipFilePath(sipId));
    }

    @Override
    public StorageStateDto getStorageState() {
        File anchor = new File(".");
        return new StorageStateDto(anchor.getTotalSpace(),anchor.getFreeSpace(),true,StorageType.FILESYSTEM);
    }

    private Path getXmlFilePath(String uuid) throws IOException {
        Path dirPath = Paths.get("xml",uuid.substring(0, 2), uuid.substring(2, 4), uuid.substring(4, 6));
        Files.createDirectories(dirPath);
        return Paths.get(dirPath.toString(), uuid);
    }

    private Path getSipFilePath(String uuid) throws IOException {
        Path dirPath = Paths.get("sip",uuid.substring(0, 2), uuid.substring(2, 4), uuid.substring(4, 6));
        Files.createDirectories(dirPath);
        return Paths.get(dirPath.toString(), uuid);
    }

    private void storeSipFile(InputStream file, String id) throws IOException {
        Files.copy(file, getSipFilePath(id));
    }

    private void storeXmlFile(InputStream file, String id) throws IOException {
        Files.copy(file, getXmlFilePath(id));
    }
}
