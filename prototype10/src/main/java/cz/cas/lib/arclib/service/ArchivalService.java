package cz.cas.lib.arclib.service;

import com.google.common.collect.Lists;
import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.XmlState;
import cz.cas.lib.arclib.dto.StorageStateDto;
import cz.cas.lib.arclib.exception.DeletedException;
import cz.cas.lib.arclib.exception.MissingObject;
import cz.cas.lib.arclib.exception.RollbackedException;
import cz.cas.lib.arclib.exception.StillProcessingException;
import cz.cas.lib.arclib.storage.StorageService;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j
public class ArchivalService {

    private StorageService storageService;
    private ArchivalAsyncService async;
    private ArchivalDbService archivalDbService;

    /**
     * Retrieves reference to AIP.
     *
     * @param sipId
     * @param all   if true reference to SIP and all XMLs is returned otherwise reference to SIP and latest XML is retrieved
     * @return reference of AIP which contains id and stream of SIP and XML/XMLs, if there are more XML to return those
     * which are rollbacked are skipped
     * @throws DeletedException         if SIP is deleted
     * @throws RollbackedException      if SIP is rollbacked or only one XML is requested and that one is rollbacked
     * @throws StillProcessingException if SIP or some of requested XML is still processing
     */
    public AipRef get(String sipId, Optional<Boolean> all) throws RollbackedException, StillProcessingException, IOException, DeletedException {
        AipSip sipEntity = archivalDbService.getAip(sipId);

        if (sipEntity.getState() == AipState.DELETED)
            throw new DeletedException(sipEntity);
        if (sipEntity.getState() == AipState.ROLLBACKED)
            throw new RollbackedException(sipEntity);
        if (sipEntity.getState() == AipState.PROCESSING)
            throw new StillProcessingException(sipEntity);

        List<AipXml> xmls = all.isPresent() && all.get() ? sipEntity.getXmls() : Lists.newArrayList(sipEntity.getLatestXml());
        Optional<AipXml> unfinishedXml = xmls.stream().filter(xml -> xml.getState() == XmlState.PROCESSING).findFirst();
        if (unfinishedXml.isPresent())
            throw new StillProcessingException(unfinishedXml.get());
        if (xmls.size() == 1 && xmls.get(0).getState() == XmlState.ROLLBACKED)
            throw new RollbackedException(xmls.get(0));

        xmls = xmls.stream().filter(xml -> xml.getState() != XmlState.ROLLBACKED).collect(Collectors.toList());
        List<InputStream> refs = null;
        try {
            refs = storageService.getAip(sipId, xmls.stream().map(xml -> xml.getVersion()).collect(Collectors.toList()).toArray(new Integer[xmls.size()]));
        } catch (IOException e) {
            log.error(String.format("Storage error has occurred during retrieval process of AIP: %s.", sipId));
            throw e;
        }
        AipRef aip = new AipRef();
        aip.setSip(new FileRef(sipEntity.getId(), refs.get(0)));
        AipXml xml;
        for (int i = 1; i < refs.size(); i++) {
            xml = xmls.get(i - 1);
            aip.addXml(new FileRef(xml.getId(), xml.getVersion(), refs.get(i)));
        }
        log.info(String.format("AIP: %s has been successfully retrieved.", sipId));
        return aip;
    }

    /**
     * Retrieves AIP XML reference.
     *
     * @param sipId
     * @param version specifies version of XML to return, by default the latest XML is returned
     * @return reference to AIP XML
     * @throws DeletedException
     * @throws RollbackedException
     * @throws StillProcessingException
     */
    public FileRef getXml(String sipId, Optional<Integer> version) throws RollbackedException, StillProcessingException, IOException {
        AipSip sipEntity = archivalDbService.getAip(sipId);
        AipXml requestedXml;
        if (version.isPresent()) {
            Optional<AipXml> xmlOpt = sipEntity.getXmls().stream().filter(xml -> xml.getVersion() == version.get()).findFirst();
            if (!xmlOpt.isPresent()) {
                log.warn(String.format("Could not find XML version: %d of AIP: %s", version.get(), sipId));
                throw new MissingObject(AipXml.class, String.format("%s version: ", sipId, version.get()));
            }
            requestedXml = xmlOpt.get();
        } else
            requestedXml = sipEntity.getLatestXml();
        if (requestedXml.getState() == XmlState.ROLLBACKED)
            throw new RollbackedException(requestedXml);
        if (requestedXml.getState() == XmlState.PROCESSING)
            throw new StillProcessingException(requestedXml);
        InputStream xmlStream = null;
        try {
            xmlStream = storageService.getXml(sipId, requestedXml.getVersion());
        } catch (IOException e) {
            log.error(String.format("Storage error has occurred during retrieval process of XML version: %d of AIP: %s.", requestedXml.getVersion(), sipId));
            throw e;
        }
        log.info(String.format("XML version: %d of AIP: %s has been successfully retrieved.", requestedXml.getVersion(), sipId));
        return new FileRef(requestedXml.getVersion(), xmlStream);
    }


    /**
     * Stores AIP parts (SIP and ARCLib XML) into Archival Storage.
     * <p>
     * Verifies that data are consistent after transfer and if not storage and database are cleared.
     * </p>
     * <p>
     * Also handles AIP versioning when whole AIP is versioned.
     * </p>
     *
     * @param sip    SIP part of AIP
     * @param aipXml ARCLib XML part of AIP
     * @param sipMD5 SIP md5 hash
     * @param xmlMD5 XML md5 hash
     * @param sipId  id of SIP
     * @return SIP ID of created AIP
     * @throws IOException
     */
    public void store(String sipId, InputStream sip, String sipMD5, InputStream aipXml, String xmlMD5) {
        String xmlId = archivalDbService.registerAipCreation(sipId, sipMD5, xmlMD5);
        async.store(sipId, sip, sipMD5, xmlId, aipXml, xmlMD5);
    }

    /**
     * Stores ARCLib AIP XML into Archival Storage.
     * <p>
     * If MD5 hash of file after upload does not match MD5 hash provided in request, the database is cleared and exception is thrown.
     *
     * @param sipId  Id of SIP to which XML belongs
     * @param xml    Stream of xml file
     * @param xmlMD5 XML md5 hash
     */
    public void updateXml(String sipId, InputStream xml, String xmlMD5) {
        AipXml xmlEntity = archivalDbService.registerXmlUpdate(sipId, xmlMD5);
        async.updateXml(sipId, xml, xmlEntity.getId(), xmlEntity.getVersion(), xmlMD5);
    }

    /**
     * Physically removes SIP from database. XMLs and data in transaction database are not removed.
     *
     * @param sipId
     * @throws IOException
     * @throws RollbackedException
     * @throws StillProcessingException
     */
    public void delete(String sipId) throws StillProcessingException, RollbackedException {
        archivalDbService.registerSipDeletion(sipId);
        this.async.delete(sipId);
    }

    /**
     * Logically removes SIP from database.
     *
     * @param sipId
     * @throws IOException
     * @throws DeletedException
     * @throws RollbackedException
     * @throws StillProcessingException
     */
    public void remove(String sipId) throws StillProcessingException, DeletedException, RollbackedException {
        archivalDbService.removeSip(sipId);
        async.remove(sipId);
    }

    /**
     * Retrieves information about AIP.
     *
     * @param sipId
     * @throws IOException
     */
    public AipSip getAipState(String sipId) throws StillProcessingException, IOException {
        AipSip aip = archivalDbService.getAip(sipId);
        if (aip.getState() == AipState.PROCESSING)
            throw new StillProcessingException(aip);
        Map<Integer, String> checksums;
        try {
            aip.setConsistent(aip.getMd5().equalsIgnoreCase(storageService.getSipMD5(aip.getId())));
            checksums = storageService.getXmlsMD5(sipId, aip.getXmls().stream().map(aipXml -> aipXml.getVersion()).collect(Collectors.toList()));
        } catch (IOException e) {
            log.error(String.format("Storage error has occurred during retrieval of MD5 hashes of AIP: %s.", sipId));
            throw e;
        }
        aip.getXmls().stream().forEach(xml -> {
            xml.setConsistent(xml.getMd5().equalsIgnoreCase(checksums.get(xml.getVersion())));
        });
        log.info(String.format("Info about AIP: %s has been successfully retrieved.", sipId));
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

    /**
     * Deletes files which are in PROCESSING state from storage and database.
     *
     * @throws IOException
     */
    public void clearUnfinished() throws IOException {
        int xmlCounter = 0;
        List<AipSip> unfinishedSips = new ArrayList<>();
        List<AipXml> unfinshedXmls = new ArrayList<>();
        archivalDbService.fillUnfinishedFilesLists(unfinishedSips, unfinshedXmls);
        for (AipSip sip :
                unfinishedSips) {
            if (sip.getXmls().size() > 1)
                log.warn("Found more than one XML of SIP package with id " + sip.getId() + " which was in PROCESSING state. SIP and all XMLS will be rollbacked.");
            storageService.deleteSip(sip.getId(), true);
            for (AipXml xml : sip.getXmls()) {
                xmlCounter++;
                storageService.deleteXml(sip.getId(), xml.getVersion());
            }
        }
        for (AipXml xml : unfinshedXmls) {
            storageService.deleteXml(xml.getSip().getId(), xml.getVersion());
        }
        archivalDbService.rollbackUnfinishedFilesRecords();
        log.info(String.format("Successfully rollbacked %d SIPs and %d XMLs", unfinishedSips.size(), xmlCounter + unfinshedXmls.size()));
    }

    @Inject
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Inject
    public void setArchivalDbService(ArchivalDbService archivalDbService) {
        this.archivalDbService = archivalDbService;
    }

    @Inject
    public void setAsyncService(ArchivalAsyncService async) {
        this.async = async;
    }
}
