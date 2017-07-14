package cz.inqool.arclib.service;

import cz.inqool.uas.store.Transactional;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional
public class FileSystemStorageService implements StorageService {

    @Override
    public void storeAip(InputStream sip, InputStream xml, String sipId, String xmlId) throws IOException {
        store(sip, sipId);
        store(xml, xmlId);
    }

    @Override
    public AipRef getAip(String sipId, String... xmlIds) throws IOException {
        AipRef aip = new AipRef();
        try (InputStream is = new FileInputStream(getFilePath(sipId).toString())) {
            aip.setSip(new FileRef(sipId, is));
        }
        for (String xmlId : xmlIds) {
            try (InputStream is = new FileInputStream(getFilePath(sipId).toString())) {
                aip.addXml(new FileRef(xmlId, is));
            }
        }
        return aip;
    }

    @Override
    public void storeXml(InputStream xml, String xmlId) throws IOException {
        store(xml, xmlId);
    }

    @Override
    public FileRef getXml(String xmlId) throws IOException {
        try (InputStream is = new FileInputStream(getFilePath(xmlId).toString())) {
            return new FileRef(xmlId, is);
        }
    }

    @Override
    public void deleteSip(String sipId) throws IOException {
        Files.delete(getFilePath(sipId));
    }

    private Path getFilePath(String uuid) throws IOException {
        Path dirPath = Paths.get(uuid.substring(0, 2), uuid.substring(2, 4), uuid.substring(4, 6));
        Files.createDirectories(dirPath);
        return Paths.get(dirPath.toString(), uuid);
    }

    private void store(InputStream file, String id) throws IOException {
        Files.copy(file, getFilePath(id));
    }
}
