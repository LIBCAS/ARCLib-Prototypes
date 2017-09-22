package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.dto.StorageStateDto;
import cz.cas.lib.arclib.dto.StoredFileInfoDto;
import cz.cas.lib.arclib.exception.ChecksumChanged;
import cz.cas.lib.arclib.exception.NotFound;
import cz.cas.lib.arclib.storage.StorageService;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArchivalService {

    private StorageService storageService;
    private ArchivalDbService archivalDbService;

    /**
     * Retrieves reference to AIP.
     *
     * @param sipId
     * @return Reference of AIP which contains id, name and stream of SIP and all its XMLs
     * @throws IOException
     */
    public AipRef get(String sipId) throws IOException, NotFound {
        AipSip sipEntity = archivalDbService.getAip(sipId);
        if(sipEntity.getState().equals(AipState.DELETED))
            throw new NotFound();
        List<AipXml> xmls = sipEntity.getXmls();
        List<InputStream> refs = storageService.getAip(sipId, xmls.stream().map(xml -> xml.getId()).collect(Collectors.toList()).toArray(new String[xmls.size()]));
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
     * <p>
     * If MD5 hashes of uploaded files do not match MD5 hashes provided in request, the database is cleared and exception is thrown.
     *
     * @param sip     Stream of SIP file
     * @param sipName SIP file name
     * @param sipMD5  SIP md5 hash
     * @param aipXml  Stream of XML file
     * @param xmlName XML file name
     * @param sipMD5  XML md5 hash
     * @return Information about both stored files containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     * @throws IOException
     * @throws ChecksumChanged if MD5 hashes of uploaded files do not match MD5 hashes provided in request
     */
    public List<StoredFileInfoDto> store(InputStream sip, String sipName, String sipMD5, InputStream aipXml, String xmlName, String xmlMD5) throws IOException, ChecksumChanged {

        String sipId = UUID.randomUUID().toString();
        String xmlId = UUID.randomUUID().toString();


        archivalDbService.registerAipCreation(sipId, sipName, sipMD5, xmlId, xmlName, xmlMD5);
        Map<String, String> storageMD5 = storageService.storeAip(sip, sipId, aipXml, xmlId);

        if(!(sipMD5.equalsIgnoreCase(storageMD5.get(sipId)) && xmlMD5.equalsIgnoreCase(storageMD5.get(xmlId)))){
            this.storageService.delete(sipId);
            this.storageService.delete(xmlId);
            this.archivalDbService.deleteAip(sipId);
            throw new ChecksumChanged("Stored files MD5 hashes do not match MD5 hashes provided in request. Make sure that provided MD5 hashes are correct and repeat the request.");
        }
        archivalDbService.finishAipCreation(sipId, xmlId);
        List<StoredFileInfoDto> fileInfos = new ArrayList<>();
        fileInfos.add(new StoredFileInfoDto(sipId, sipName));
        fileInfos.add(new StoredFileInfoDto(xmlId, xmlName));
        return fileInfos;
    }

    /**
     * Stores ARCLib AIP XML into Archival Storage and its metadata to transaction database.
     * <p>
     * If MD5 hash of file after upload does not match MD5 hash provided in request, the database is cleared and exception is thrown.
     *
     * @param sipId   Id of SIP to which XML belongs
     * @param xmlName Name of xml file
     * @param xml     Stream of xml file
     * @param xmlMD5  XML md5 hash
     * @return Information about stored xml containing file name, its assigned id and boolean flag determining whether the stored file is consistent i.e. was not changed during the transfer.
     * @throws IOException
     * @throws ChecksumChanged if MD5 hash of file after upload does not match MD5 hash provided in request
     */
    public StoredFileInfoDto updateXml(String sipId, String xmlName, InputStream xml, String xmlMD5) throws IOException, ChecksumChanged {
        String xmlId = UUID.randomUUID().toString();
        archivalDbService.registerXmlUpdate(sipId, xmlId, xmlName, xmlMD5);
        String storageMD5 = storageService.storeXml(xml, xmlId);
        if(!xmlMD5.equalsIgnoreCase(storageMD5)){
            this.storageService.delete(xmlId);
            this.archivalDbService.deleteXml(xmlId);
            throw new ChecksumChanged("Stored file MD5 hash does not match MD5 hash provided in request. Make sure that provided MD5 hash is correct and repeat the request.");
        }
        archivalDbService.finishXmlProcess(xmlId);
        InputStream xmlRef = storageService.getXml(xmlId);

        StoredFileInfoDto fileInfo = new StoredFileInfoDto(xmlId, xmlName);
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
        storageService.delete(sipId);
        archivalDbService.finishSipDeletion(sipId);
    }

    /**
     * Retrieves information about AIP.
     *
     * @param uuid id of AIP
     * @throws IOException
     */
    public AipSip getAipInfo(String uuid) throws IOException {
        AipSip aip = archivalDbService.getAip(uuid);
        Map<String, String> checksums;
        if(aip.getState().equals(AipState.DELETED)){
           checksums = storageService.getMD5(null, aip.getXmls().stream().map(aipXml -> aipXml.getId()).collect(Collectors.toList()));
        }
        else{
            checksums = storageService.getMD5(uuid, aip.getXmls().stream().map(aipXml -> aipXml.getId()).collect(Collectors.toList()));
            aip.setConsistent(checksums.get(uuid).equalsIgnoreCase(aip.getMd5()));
        }
        aip.getXmls().stream().forEach(xml -> {
            xml.setConsistent(checksums.get(xml.getId()).equalsIgnoreCase(xml.getMd5()));
        });
        return aip;
    }

    /**
     * Returns state of currently used storage.
     *
     * @return
     */
    public StorageStateDto getStorageState() {
        return storageService.getStorageState();
    }

    @Inject
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Inject
    public void setArchivalDbService(ArchivalDbService archivalDbService) {
        this.archivalDbService = archivalDbService;
    }
}
