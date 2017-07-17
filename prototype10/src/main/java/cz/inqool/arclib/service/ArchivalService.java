package cz.inqool.arclib.service;

import cz.inqool.arclib.fixity.FixityCounter;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.util.List;
import java.util.UUID;

@Service
public class ArchivalService {

    private StorageService storageService;
    private FixityCounter fixityCounter;
    private ArchivalDbService archivalDbService;

    public AipRef get(String sipId) throws IOException {
        List<String> xmlIds = archivalDbService.getXmls(sipId);
        return storageService.getAip(sipId, xmlIds.toArray(new String[xmlIds.size()]));
    }

    public boolean store(InputStream sip, InputStream aipXml, InputStream meta) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(meta));
        String sipHash = br.readLine();
        String xmlHash = br.readLine();

        String sipId = UUID.randomUUID().toString();
        String xmlId = UUID.randomUUID().toString();

        archivalDbService.registerAipCreation(sipId, xmlId, sipHash, xmlHash);
        storageService.storeAip(sip, aipXml, sipId, xmlId);
        AipRef aip = storageService.getAip(sipId, xmlId);
        boolean consistent = fixityCounter.verifyFixity(aip.getSip().getStream(), sipHash) && fixityCounter.verifyFixity(aip.getXml(0).getStream(), xmlHash);
        if (consistent)
            archivalDbService.finishAipCreation(sipId, xmlId);
        IOUtils.closeQuietly(aip.getSip().getStream());
        IOUtils.closeQuietly(aip.getXml(0).getStream());
        return consistent;
    }

    public boolean updateXml(String sipId, InputStream xml, InputStream meta) throws IOException {
        String xmlHash = null;
        if (meta != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(meta));
            xmlHash = br.readLine();
        }
        String xmlId = UUID.randomUUID().toString();
        archivalDbService.registerXmlUpdate(sipId, xmlId, xmlHash);
        storageService.storeXml(xml, xmlId);
        if (xmlHash == null) {
            archivalDbService.finishXmlProcess(xmlId);
            return true;
        }
        FileRef file = storageService.getXml(xmlId);
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

    public void remove(String sipId) {
        archivalDbService.removeSip(sipId);
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
