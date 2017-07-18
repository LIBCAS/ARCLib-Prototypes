package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.dto.StoredFileInfoDto;
import cz.inqool.arclib.fixity.FixityCounter;
import cz.inqool.arclib.storage.StorageService;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ArchivalService {

    private StorageService storageService;
    private FixityCounter fixityCounter;
    private ArchivalDbService archivalDbService;

    /**
     * Retrieves reference to AIP.
     *
     * @param sipId
     * @return Reference of AIP which contains id, name and stream of SIP and all its XMLs
     * @throws IOException
     */
    public AipRef get(String sipId) throws IOException {
        AipSip sipEntity = archivalDbService.getAip(sipId);
        List<InputStream> refs = storageService.getAip(sipId, (String[]) sipEntity.getXmls().stream().map(xml -> xml.getId()).toArray());
        AipRef aip = new AipRef();
        aip.setSip(new FileRef(sipEntity.getId(), sipEntity.getName(), refs.get(0)));
        AipXml xml;
        for (int i = 1; i < refs.size(); i++) {
            xml = sipEntity.getXml(i - 1);
            aip.addXml(new FileRef(xml.getId(), xml.getName(), refs.get(i)));
        }
        return aip;
    }

    /**
     * Stores AIP to Archival Storage and its metadata to transaction database.
     *
     * @param sip     Stream of SIP file
     * @param sipName SIP file name
     * @param aipXml  Stream of XML file
     * @param xmlName XML file name
     * @param meta    Stream containing two lines, first is SIP md5 hash, second is XML md5 hash
     * @return Information about both stored files containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     * @throws IOException
     */
    public List<StoredFileInfoDto> store(InputStream sip, String sipName, InputStream aipXml, String xmlName, InputStream meta) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(meta));
        String sipHash = br.readLine();
        String xmlHash = br.readLine();

        String sipId = UUID.randomUUID().toString();
        String xmlId = UUID.randomUUID().toString();

        archivalDbService.registerAipCreation(sipId, sipName, sipHash, xmlId, xmlName, xmlHash);
        storageService.storeAip(sip, sipId, aipXml, xmlId);
        archivalDbService.finishAipCreation(sipId, xmlId);
        List<InputStream> refs = storageService.getAip(sipId, xmlId);
        List<StoredFileInfoDto> fileInfos = new ArrayList<>();
        fileInfos.add(new StoredFileInfoDto(sipId, sipName, fixityCounter.verifyFixity(refs.get(0), sipHash)));
        fileInfos.add(new StoredFileInfoDto(xmlId, xmlName, fixityCounter.verifyFixity(refs.get(1), xmlHash)));
        IOUtils.closeQuietly(refs.get(0));
        IOUtils.closeQuietly(refs.get(1));
        return fileInfos;
    }

    /**
     * Stores ARCLib AIP XML into Archival Storage and its metadata to transaction database.
     *
     * @param sipId   Id of SIP to which XML belongs
     * @param xmlName Name of xml file
     * @param xml     Stream of xml file
     * @param meta    Stream containing one line with ARCLib XML MD5 hash
     * @return Information about stored xml containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     * @throws IOException
     */
    public StoredFileInfoDto updateXml(String sipId, String xmlName, InputStream xml, InputStream meta) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(meta));
        String xmlHash = br.readLine();
        String xmlId = UUID.randomUUID().toString();
        archivalDbService.registerXmlUpdate(sipId, xmlId, xmlName, xmlHash);
        storageService.storeXml(xml, xmlId);
        archivalDbService.finishXmlProcess(xmlId);
        InputStream xmlRef = storageService.getXml(xmlId);
        StoredFileInfoDto fileInfo = new StoredFileInfoDto(xmlId, xmlName, fixityCounter.verifyFixity(xmlRef, xmlHash));
        IOUtils.closeQuietly(xmlRef);
        return fileInfo;
    }

    /**
     * Physically removes SIP from database. XMLs and data in transaction database are not removed.
     *
     * @param sipId
     * @throws IOException
     */
    public void delete(String sipId) throws IOException {
        archivalDbService.registerSipDeletion(sipId);
        storageService.deleteSip(sipId);
        archivalDbService.finishSipDeletion(sipId);
    }

    @Inject
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Inject
    public void setFixityCounter(FixityCounter fixityCounter) {
        this.fixityCounter = fixityCounter;
    }

    @Inject
    public void setArchivalDbService(ArchivalDbService archivalDbService) {
        this.archivalDbService = archivalDbService;
    }
}
