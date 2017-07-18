package cz.inqool.arclib.storage;

import cz.inqool.arclib.dto.StorageStateDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public interface StorageService {
    /**
     * Stores Aip files into storage.
     *
     * @param sip
     * @param sipId
     * @param xml
     * @param xmlId
     * @throws IOException
     */
    void storeAip(InputStream sip, String sipId, InputStream xml, String xmlId) throws IOException;

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
     * @throws IOException
     */
    void storeXml(InputStream xml, String xmlId) throws IOException;

    /**
     * Retrieves reference to AipXml file. Caller is responsible for closing retrieved stream.
     *
     * @param xmlId
     * @return opened input stream to AipXml file
     * @throws IOException
     */
    InputStream getXml(String xmlId) throws IOException;

    /**
     * Deletes AipSip file from storage.
     *
     * @param sipId
     * @throws IOException
     */
    void deleteSip(String sipId) throws IOException;

    /**
     * Returns state of currently used storage.
     *
     * @return
     */
    StorageStateDto getStorageState();
}
