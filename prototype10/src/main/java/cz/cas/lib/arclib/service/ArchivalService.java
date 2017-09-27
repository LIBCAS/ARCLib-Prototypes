package cz.cas.lib.arclib.service;

import com.google.common.collect.Lists;
import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.dto.AipCreationMd5Info;
import cz.cas.lib.arclib.dto.StorageStateDto;
import cz.cas.lib.arclib.exception.ChecksumChanged;
import cz.cas.lib.arclib.exception.NotFound;
import cz.cas.lib.arclib.storage.StorageService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * @param all   if true reference to SIP and all XMLs is returned otherwise reference to SIP and latest XML is retrieved
     * @return reference of AIP which contains ids and streams of its parts
     * @throws IOException
     * @throws NotFound if AIP does not exist or is in {@link cz.cas.lib.arclib.domain.AipState#DELETED} state
     */
    public AipRef get(String sipId, Optional<Boolean> all) throws IOException, NotFound {
        AipSip sipEntity = archivalDbService.getAip(sipId);
        if (sipEntity.getState().equals(AipState.DELETED))
            throw new NotFound();
        List<AipXml> xmls = all.isPresent() && all.get() ? sipEntity.getXmls() : Lists.newArrayList(sipEntity.getLatestXml());
        List<InputStream> refs = storageService.getAip(sipId, xmls.stream().map(xml -> xml.getVersion()).collect(Collectors.toList()).toArray(new Integer[xmls.size()]));

        AipRef aip = new AipRef();
        aip.setSip(new FileRef(sipEntity.getId(), refs.get(0)));
        AipXml xml;
        for (int i = 1; i < refs.size(); i++) {
            xml = xmls.get(i - 1);
            aip.addXml(new FileRef(xml.getId(), xml.getVersion(), refs.get(i)));
        }
        return aip;
    }

    /**
     * Retrieves AIP XML reference.
     * @param sipId
     * @param version   specifies version of XML to return, by default the latest XML is returned
     * @return  reference to AIP XML
     * @throws IOException
     * @throws NotFound if either AIP does not exists or specified version of XML does not exist
     */
    public FileRef getXml(String sipId, Optional<Integer> version) throws IOException, NotFound {
        AipSip sipEntity = archivalDbService.getAip(sipId);
        int v;
        if (version.isPresent()) {
            if (!sipEntity.getXmls().stream().anyMatch(xml -> xml.getVersion() == version.get()))
                throw new NotFound();
            v = version.get();
        } else
            v = sipEntity.getLatestXml().getVersion();
        InputStream xmlStream = storageService.getXml(sipId, v);
        return new FileRef(v, xmlStream);
    }


    /**
     * Stores AIP parts (SIP and ARCLib XML) into Archival Storage.
     * <p>
     * Verifies that data are consistent after transfer and if not storage and database are cleared.
     * </p>
     * <p>
     * Also handles AIP versioning when whole AIP is versioned.
     * </p>
     * @param sip    SIP part of AIP
     * @param aipXml    ARCLib XML part of AIP
     * @param sipMD5 SIP md5 hash
     * @param xmlMD5 XML md5 hash
     * @param id    optional parameter, if not specified id is generated
     * @return SIP ID of created AIP
     * @throws IOException
     * @throws ChecksumChanged  if provided MD5 checksums does not match checksums of stored files
     */
    public String store(InputStream sip, String sipMD5, InputStream aipXml, String xmlMD5, Optional<String> id) throws IOException, ChecksumChanged {
        String sipId = id.isPresent() ? id.get() : UUID.randomUUID().toString();
        String xmlId = archivalDbService.registerAipCreation(sipId, sipMD5, xmlMD5);

        AipCreationMd5Info creationMd5Info = storageService.storeAip(sip, sipId, aipXml);

        if (!(sipMD5.equalsIgnoreCase(creationMd5Info.getSipMd5()) && xmlMD5.equalsIgnoreCase(creationMd5Info.getXmlMd5()))) {
            storageService.deleteSip(sipId);
            storageService.deleteXml(sipId, 1);
            archivalDbService.deleteAip(sipId);
            throw new ChecksumChanged("Stored files MD5 hashes do not match MD5 hashes provided in request. Make sure that provided MD5 hashes are correct and repeat the request.");
        }
        archivalDbService.finishAipCreation(sipId, xmlId);
        return sipId;
    }

    /**
     * Stores ARCLib AIP XML into Archival Storage.
     * <p>
     * If MD5 hash of file after upload does not match MD5 hash provided in request, the database is cleared and exception is thrown.
     * @param sipId  Id of SIP to which XML belongs
     * @param xml    Stream of xml file
     * @param xmlMD5 XML md5 hash
     * @throws IOException
     * @throws ChecksumChanged if MD5 hash of file after upload does not match MD5 hash provided in request
     */
    public void updateXml(String sipId, InputStream xml, String xmlMD5) throws IOException, ChecksumChanged {
        AipXml xmlEntity = archivalDbService.registerXmlUpdate(sipId, xmlMD5);
        String storageMD5 = storageService.storeXml(xml, sipId, xmlEntity.getVersion());
        if (!xmlMD5.equalsIgnoreCase(storageMD5)) {
            this.storageService.deleteXml(sipId, xmlEntity.getVersion());
            this.archivalDbService.deleteXml(xmlEntity.getId());
            throw new ChecksumChanged("Stored file MD5 hash does not match MD5 hash provided in request. Make sure that provided MD5 hash is correct and repeat the request.");
        }
        archivalDbService.finishXmlProcess(xmlEntity.getId());
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

    /**
     * Logically removes SIP from database.
     *
     * @param sipId
     * @throws IOException
     */
    public void remove(String sipId) throws IOException {
        archivalDbService.removeSip(sipId);
        storageService.remove(sipId);
    }

    /**
     * Retrieves information about AIP.
     *
     * @param uuid id of AIP
     * @throws IOException
     */
    public AipSip getAipInfo(String uuid) throws IOException {
        AipSip aip = archivalDbService.getAip(uuid);
        Map<Integer, String> checksums;
        if (!aip.getState().equals(AipState.DELETED))
            aip.setConsistent(storageService.getSipMD5(aip.getId()).equalsIgnoreCase(aip.getMd5()));
        checksums = storageService.getXmlsMD5(uuid, aip.getXmls().stream().map(aipXml -> aipXml.getVersion()).collect(Collectors.toList()));
        aip.getXmls().stream().forEach(xml -> {
            xml.setConsistent(checksums.get(xml.getVersion()).equalsIgnoreCase(xml.getMd5()));
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
