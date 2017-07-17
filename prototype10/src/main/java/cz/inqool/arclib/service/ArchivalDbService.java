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
/**
 * Class used for communication with Archival Storage database which contains transactional data about Archival Storage packages.
 */
public class ArchivalDbService {

    private AipSipStore aipSipStore;
    private AipXmlStore aipXmlStore;

    /**
     * Registers that AIP creation process has started. Stores AIP records to database and sets their state to <i>processing</i>.
     * @param sipId
     * @param sipName
     * @param sipHash
     * @param xmlId
     * @param xmlName
     * @param xmlHash
     */
    public void registerAipCreation(String sipId, String sipName, String sipHash, String xmlId, String xmlName, String xmlHash) {
        AipSip sip = new AipSip(sipId,sipName,sipHash,AipState.PROCESSING);
        AipXml xml = new AipXml(xmlId,xmlName,xmlHash,sip,1,true);
        aipSipStore.save(sip);
        aipXmlStore.save(xml);
    }

    /**
     * Registers that AIP creation process has ended.
     * @param sipId
     * @param xmlId
     */
    public void finishAipCreation(String sipId, String xmlId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.ARCHIVED);
        aipSipStore.save(sip);
        finishXmlProcess(xmlId);
    }

    /**
     * Registers that AIP XML update process has started.
     * @param sipId
     * @param xmlId
     * @param xmlName
     * @param xmlHash
     */
    public void registerXmlUpdate(String sipId, String xmlId, String xmlName, String xmlHash) {
        AipXml xml = new AipXml(xmlId,xmlName,xmlHash,new AipSip(sipId),aipXmlStore.getNextXmlVersionNumber(sipId),true);
        aipXmlStore.save(xml);
    }

    /**
     * Registers that AIP SIP deletion process has started.
     * @param sipId
     */
    public void registerSipDeletion(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.PROCESSING);
        aipSipStore.save(sip);
    }

    /**
     * Registers that AIP SIP deletion process has ended.
     * @param sipId
     */
    public void finishSipDeletion(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.DELETED);
        aipSipStore.save(sip);
    }

    /**
     * Registers that process which used AIP XML file has ended.
     * @param xmlId
     */
    public void finishXmlProcess(String xmlId) {
        AipXml xml = aipXmlStore.find(xmlId);
        notNull(xml, () -> new MissingObject(AipXml.class, xmlId));
        xml.setProcessing(false);
        aipXmlStore.save(xml);
    }

    /**
     * Logically removes SIP i.e. sets its state to <i>removed</i> in the database.
     * @param sipId
     */
    public void removeSip(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        sip.setState(AipState.REMOVED);
        aipSipStore.save(sip);
    }

    /**
     * Retrieves AipSip entity.
     * @param sipId
     * @param populate  whether to populate entity with xmls and therefore retrieve information about whole AIP or don't populate and therefore retrieve information about SIP part of AIP
     * @return  AipSip entity with either populated or non-populated list of xmls
     */
    public AipSip getAip(String sipId, boolean populate) {
        AipSip sip = aipSipStore.find(sipId);
        if(populate)
            sip.getXmls().size();
        return sip;
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
