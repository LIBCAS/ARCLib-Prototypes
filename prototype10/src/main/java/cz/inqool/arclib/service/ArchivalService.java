package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.fixity.FixityCounter;
import cz.inqool.arclib.storage.StorageService;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

@Service
public class ArchivalService {

    private StorageService storageService;
    private FixityCounter fixityCounter;
    private ArchivalDbService archivalDbService;

    public AipRef get(String sipId) throws IOException {
        AipSip sipEntity = archivalDbService.getAip(sipId,true);
        List<InputStream> refs = storageService.getAip(sipId, (String[])sipEntity.getXmls().stream().map(xml -> xml.getId()).toArray());
        AipRef aip = new AipRef();
        aip.setSip(new FileRef(sipEntity.getId(), sipEntity.getName(), refs.get(0)));
        AipXml xml;
        for (int i = 1; i < refs.size(); i++) {
            xml = sipEntity.getXml(i - 1);
            aip.addXml(new FileRef(xml.getId(), xml.getName(), refs.get(i)));
        }
        return aip;
    }

    public boolean store(InputStream sip, String sipName, InputStream aipXml, String xmlName, InputStream meta) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(meta));
        String sipHash = br.readLine();
        String xmlHash = br.readLine();

        String sipId = UUID.randomUUID().toString();
        String xmlId = UUID.randomUUID().toString();

        archivalDbService.registerAipCreation(sipId, sipName, sipHash, xmlId, xmlName, xmlHash);
        storageService.storeAip(sip, sipId, aipXml, xmlId);
        List<InputStream> refs = storageService.getAip(sipId, xmlId);
        AipRef aip = new AipRef();
        aip.setSip(new FileRef(sipId, sipName, refs.get(0)));
        aip.addXml(new FileRef(xmlId, xmlName, refs.get(1)));
        boolean consistent = fixityCounter.verifyFixity(aip.getSip().getStream(), sipHash) && fixityCounter.verifyFixity(aip.getXml(0).getStream(), xmlHash);
        if (consistent)
            archivalDbService.finishAipCreation(sipId, xmlId);
        IOUtils.closeQuietly(aip.getSip().getStream());
        IOUtils.closeQuietly(aip.getXml(0).getStream());
        return consistent;
    }

    public boolean updateXml(String sipId, String xmlName, InputStream xml, InputStream meta) throws IOException {
        String xmlHash = null;
        if (meta != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(meta));
            xmlHash = br.readLine();
        }
        String xmlId = UUID.randomUUID().toString();
        archivalDbService.registerXmlUpdate(sipId, xmlId, xmlName, xmlHash);
        storageService.storeXml(xml, xmlId);
        if (xmlHash == null) {
            archivalDbService.finishXmlProcess(xmlId);
            return true;
        }
        FileRef file = new FileRef(xmlId, xmlName, storageService.getXml(xmlId));
        boolean consistent = fixityCounter.verifyFixity(file.getStream(), xmlHash);
        if (consistent)
            archivalDbService.finishXmlProcess(xmlId);
        IOUtils.closeQuietly(file.getStream());
        return consistent;
    }

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
