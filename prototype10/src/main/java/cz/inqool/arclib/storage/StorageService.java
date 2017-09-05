package cz.inqool.arclib.storage;

import cz.inqool.arclib.dto.StorageStateDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


public interface StorageService {
    /**
     * Stores Aip files into storage.
     *
     * @param sip
     * @param sipId
     * @param xml
     * @param xmlId
     * @return Map with SIP and XML files ids as keys and their MD5 checksums as values.
     * @throws IOException
     */
    Map<String, String> storeAip(InputStream sip, String sipId, InputStream xml, String xmlId) throws IOException;

    /**
     * Retrieves references to Aip files. Caller is responsible for closing retrieved streams.
     *
     * @param sipId
     * @param xmlIds
     * @return List of opened input streams to Aip files in order specified by input params i.e. first is reference to AipSip and then references to AipXmls.
     * @throws IOException
     */
    List<InputStream> getAip(String sipId, String... xmlIds) throws IOException;

    /**
     * Stores AipXml files into storage.
     *
     * @param xml   opened input stream to xml file
     * @param xmlId
     * @return MD5 hash of stored file
     * @throws IOException
     */
    String storeXml(InputStream xml, String xmlId) throws IOException;

    /**
     * Retrieves reference to AipXml file. Caller is responsible for closing retrieved stream.
     *
     * @param xmlId
     * @return opened input stream to AipXml file
     * @throws IOException
     */
    InputStream getXml(String xmlId) throws IOException;

    /**
     * Deletes file (SIP or XML) from storage.
     *
     * @param id
     * @throws IOException
     */
    void delete(String id) throws IOException;

    /**
     * Computes and retrieves MD5 checksums of Aip SIP and XML files.
     *
     * @param sipId if null only checksums of XML files are computed
     * @param xmlIds
     * @return Map with ids of Aip SIP and XML files as keys and MD5 strings with fixity information as values.
     * @throws IOException
     */
    Map<String, String> getMD5(String sipId, List<String> xmlIds) throws IOException;

    /**
     * Returns state of currently used storage.
     *
     * @return
     */
    StorageStateDto getStorageState();
}
