package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.exception.ConflictObject;
import cz.cas.lib.arclib.exception.MissingObject;
import cz.cas.lib.arclib.store.AipSipStore;
import cz.cas.lib.arclib.store.AipXmlStore;
import cz.cas.lib.arclib.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.util.Collections;
import java.util.Map;

import static cz.cas.lib.arclib.util.Utils.notNull;


/**
 * Class used for communication with Archival Storage database which contains transactional data about Archival Storage packages.
 */
@Service
@Transactional
public class ArchivalDbService {

    private AipSipStore aipSipStore;
    private AipXmlStore aipXmlStore;

    /**
     * Registers that AIP creation process has started. Stores AIP records to database and sets their state to <i>processing</i>.
     *
     * @param sipId
     * @param sipHash
     * @param xmlHash
     * @return  generated ID of XML record
     */
    public String registerAipCreation(String sipId, String sipHash,String xmlHash) {
        notNull(sipId, () -> new BadArgument(sipId));
        AipSip sip = aipSipStore.find(sipId);
        if (sip != null)
            throw new ConflictObject(sip);
        sip = new AipSip(sipId, sipHash, AipState.PROCESSING);
        AipXml xml = new AipXml(xmlHash, sip, 1, true);
        aipSipStore.save(sip);
        aipXmlStore.save(xml);
        return xml.getId();
    }

    /**
     * Registers that AIP creation process has ended.
     *
     * @param sipId
     * @param xmlId
     * @throws IllegalStateException if {@link AipSip#state} is not {@link AipState#PROCESSING} or {@link AipXml#processing} is false
     */
    public void finishAipCreation(String sipId,String xmlId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        if (sip.getState() != AipState.PROCESSING)
            throw new IllegalStateException("SIP: " + sipId + " creation can't be finished because SIP is not in " + AipState.PROCESSING + " state. Current state: " + sip.getState());
        sip.setState(AipState.ARCHIVED);
        aipSipStore.save(sip);
        finishXmlProcess(xmlId);
    }

    /**
     * Registers that AIP XML update process has started.
     *
     * @param sipId
     * @param xmlHash
     * @return  created XML entity filled with generated ID and version
     */
    public AipXml registerXmlUpdate(String sipId, String xmlHash) {
        notNull(sipId, () -> new BadArgument(sipId));
        int xmlVersion = aipXmlStore.getNextXmlVersionNumber(sipId);
        AipXml newVersion = new AipXml(xmlHash, new AipSip(sipId), xmlVersion, true);
        aipXmlStore.save(newVersion);
        return newVersion;
    }

    /**
     * Registers that AIP SIP deletion process has started.
     *
     * @param sipId
     * @throws IllegalStateException if {@link AipSip#state} is {@link AipState#PROCESSING}
     */
    public void registerSipDeletion(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        if (sip.getState() == AipState.PROCESSING)
            throw new IllegalStateException("SIP: " + sipId + " deletion can't be started because SIP is in " + AipState.PROCESSING + " state.");
        sip.setState(AipState.PROCESSING);
        aipSipStore.save(sip);
    }

    /**
     * Registers that AIP SIP deletion process has ended.
     *
     * @param sipId
     * @throws IllegalStateException if {@link AipSip#state} is not {@link AipState#PROCESSING}
     */
    public void finishSipDeletion(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        if (sip.getState() != AipState.PROCESSING)
            throw new IllegalStateException("SIP: " + sipId + " deletion can't be finished because SIP is not in " + AipState.PROCESSING + " state. Current state: " + sip.getState());
        sip.setState(AipState.DELETED);
        aipSipStore.save(sip);
    }

    /**
     * Registers that process which used AIP XML file has ended.
     *
     * @param xmlId
     * @throws IllegalArgumentException if {@link AipXml#processing} is false
     */
    public void finishXmlProcess(String xmlId) {
        AipXml xml = aipXmlStore.find(xmlId);
        notNull(xml, () -> new MissingObject(AipXml.class, xmlId));
        if (!xml.isProcessing())
            throw new IllegalStateException("XML " + xmlId + " process has already finished");
        xml.setProcessing(false);
        aipXmlStore.save(xml);
    }

    /**
     * Logically removes SIP i.e. sets its state to {@link AipState#REMOVED} in the database.
     *
     * @param sipId
     * @throws IllegalArgumentException if {@link AipSip#state} is not {@link AipState#ARCHIVED} or {@link AipState#REMOVED}
     */
    public void removeSip(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        if (sip.getState() != AipState.ARCHIVED)
            throw new IllegalStateException("SIP: " + sipId + " can't be logically removed because it is not in " + AipState.ARCHIVED + "/" + AipState.REMOVED + " state. Current state: " + sip.getState());
        sip.setState(AipState.REMOVED);
        aipSipStore.save(sip);
    }

    /**
     * Retrieves AipSip entity.
     *
     * @param sipId
     * @return AipSip entity with populated list of xmls
     */
    public AipSip getAip(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> new MissingObject(AipSip.class, sipId));
        return sip;
    }

    /**
     * Removes record about SIP and all related XMLs from database. Should be used only when the AIP creation process fails.
     *
     * @param id
     */
    public void deleteAip(String id) {
        aipSipStore.delete(aipSipStore.find(id));
    }

    /**
     * Removes record about XML from database. Should be used only when the XML update process fails.
     *
     * @param id
     */
    public void deleteXml(String id) {
        aipXmlStore.delete(aipXmlStore.find(id));
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
