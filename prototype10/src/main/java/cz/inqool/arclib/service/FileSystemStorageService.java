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
        storeSipFile(sip, sipId);
        storeXmlFile(xml, xmlId);
    }

    @Override
    public AipRef getAip(String sipId, String... xmlIds) throws IOException {
        AipRef aip = new AipRef();
        aip.setSip(new FileRef(sipId, new FileInputStream(getSipFilePath(sipId).toString())));
        for (String xmlId : xmlIds) {
            aip.addXml(new FileRef(xmlId, new FileInputStream(getXmlFilePath(xmlId).toString())));
        }
        return aip;
    }

    @Override
    public void storeXml(InputStream xml, String xmlId) throws IOException {
        storeXmlFile(xml, xmlId);
    }

    @Override
    public FileRef getXml(String xmlId) throws IOException {
        return new FileRef(xmlId, new FileInputStream(getXmlFilePath(xmlId).toString()));
    }

    @Override
    public void deleteSip(String sipId) throws IOException {
        Files.delete(getSipFilePath(sipId));
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
