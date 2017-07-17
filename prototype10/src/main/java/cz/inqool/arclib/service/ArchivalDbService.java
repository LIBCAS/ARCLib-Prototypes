package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.store.AipSipStore;
import cz.inqool.arclib.store.AipXmlStore;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

import static cz.inqool.uas.util.Utils.notNull;

@Service
@Transactional
public class ArchivalDbService {

    private AipSipStore aipSipStore;
    private AipXmlStore aipXmlStore;

    public void registerAipCreation(String sipId, String xmlId, String sipHash, String xmlHash) {
        AipSip sip = new AipSip();
        sip.setId(sipId);
        sip.setState(AipState.PROCESSING);
        sip.setMd5(sipHash);
        AipXml xml = new AipXml();
        xml.setId(xmlId);
        xml.setVersion(1);
        xml.setProcessing(true);
        xml.setMd5(xmlHash);
        xml.setSip(sip);
        aipSipStore.save(sip);
        aipXmlStore.save(xml);
    }

    public void finishAipCreation(String sipId, String xmlId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.ARCHIVED);
        aipSipStore.save(sip);
        finishXmlProcess(xmlId);
    }

    public void registerXmlUpdate(String sipId, String xmlId, String xmlHash) {
        AipXml xml = new AipXml();
        xml.setId(xmlId);
        xml.setVersion(aipXmlStore.getNextXmlVersionNumber(sipId));
        xml.setProcessing(true);
        xml.setMd5(xmlHash);
        xml.setSip(new AipSip(sipId));
        aipXmlStore.save(xml);
    }

    public void registerSipDeletion(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.PROCESSING);
        aipSipStore.save(sip);
    }

    public void finishSipDeletion(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.DELETED);
        aipSipStore.save(sip);
    }

    public void finishXmlProcess(String xmlId) {
        AipXml xml = aipXmlStore.find(xmlId);
        notNull(xml, () -> new MissingObject(AipXml.class, xmlId));
        xml.setProcessing(false);
        aipXmlStore.save(xml);
    }

    public void removeSip(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.REMOVED);
        aipSipStore.save(sip);
    }

    public List<String> getXmls(String sipId) {
        return aipXmlStore.getXmlIds(sipId);
    }

    @Inject
    public void setAipSipStore(AipSipStore store) {
        this.aipSipStore = store;
    }

    @Inject
    public void setAipXmlStore(AipXmlStore store) {
        this.aipXmlStore = store;
    }
}
