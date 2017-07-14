package cz.inqool.arclib.service;

import java.io.IOException;
import java.io.InputStream;


public interface StorageService {
    void storeAip(InputStream sip, InputStream xml, String sipId, String xmlId) throws IOException;

    AipRef getAip(String sipId, String... xmlIds) throws IOException;

    void storeXml(InputStream xml, String xmlId) throws IOException;

    FileRef getXml(String xmlId) throws IOException;

    void deleteSip(String sipId) throws IOException;
}
