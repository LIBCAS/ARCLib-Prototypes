package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.dto.AipCreationMd5Info;
import cz.cas.lib.arclib.storage.StorageService;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.IOUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Service
@Log4j
public class ArchivalAsyncService {

    private StorageService storageService;
    private ArchivalDbService archivalDbService;

    @Async
    public void store(String sipId, InputStream sip, String sipMD5, String xmlId, InputStream aipXml, String xmlMD5) {
        AipCreationMd5Info creationMd5Info;
        try {
            creationMd5Info = storageService.storeAip(sip, sipId, aipXml);
        } catch (IOException e) {
            log.error(String.format("Storage error has occurred during store process of AIP: %s.", sipId));
            throw new UncheckedIOException(e);
        } finally {
            IOUtils.closeQuietly(sip);
            IOUtils.closeQuietly(aipXml);
        }
        if (!(sipMD5.equalsIgnoreCase(creationMd5Info.getSipMd5()) && xmlMD5.equalsIgnoreCase(creationMd5Info.getXmlMd5()))) {
            log.warn(String.format("Stored files MD5 hashes: sip:%s xml:%s do not match MD5 hashes provided in request: sip:%s xml:%s SIP ID:%s", creationMd5Info.getSipMd5(), creationMd5Info.getXmlMd5(), sipMD5, xmlMD5, sipId));
            try {
                storageService.deleteSip(sipId, true);
                storageService.deleteXml(sipId, 1);
            } catch (IOException e) {
                log.error(String.format("Storage error has occurred during rollback process of AIP: %s.", sipId));
                throw new UncheckedIOException(e);
            }
            archivalDbService.rollbackSip(sipId, xmlId);
            return;
        }
        archivalDbService.finishAipCreation(sipId, xmlId);
        log.info(String.format("AIP: %s has been successfully stored.", sipId));
    }

    @Async
    public void updateXml(String sipId, InputStream xml, String xmlId, int version, String xmlMD5) {
        String storageMD5;
        try {
            storageMD5 = storageService.storeXml(xml, sipId, version);
        } catch (IOException e) {
            log.error(String.format("Storage error has occurred during store process of XML version: %d of AIP: %s.", version, sipId));
            throw new UncheckedIOException(e);
        } finally {
            IOUtils.closeQuietly(xml);
        }
        if (!xmlMD5.equalsIgnoreCase(storageMD5)) {
            log.warn(String.format("Stored file MD5 hash:%s do not match MD5 hash provided in request:%s SIP ID:%s", storageMD5, xmlMD5, sipId));
            try {
                this.storageService.deleteXml(sipId, version);
            } catch (IOException e) {
                log.error(String.format("Storage error has occurred during rollback process of XML version: %d of AIP: %s.", version, sipId));
                throw new UncheckedIOException(e);
            }
            this.archivalDbService.rollbackXml(xmlId);
            return;
        }
        archivalDbService.finishXmlProcess(xmlId);
        log.info(String.format("XML version: %d of AIP: %s has been successfully stored.", version, sipId));
    }

    @Async
    public void delete(String sipId) {
        try {
            storageService.deleteSip(sipId, false);
        } catch (IOException e) {
            log.error(String.format("Storage error has occurred during deletion of AIP: %s.", sipId));
            throw new UncheckedIOException(e);
        }
        archivalDbService.finishSipDeletion(sipId);
        log.info(String.format("AIP: %s has been successfully deleted.", sipId));
    }

    @Async
    public void remove(String sipId) {
        try {
            storageService.remove(sipId);
        } catch (IOException e) {
            log.error(String.format("Storage error has occurred during removal of AIP: %s. AIP is marked as removed in DB but may not be marked as removed on every storage.", sipId));
            throw new UncheckedIOException(e);
        }
        log.info(String.format("AIP: %s has been successfully removed.", sipId));
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
