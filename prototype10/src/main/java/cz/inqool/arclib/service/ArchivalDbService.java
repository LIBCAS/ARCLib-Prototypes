package cz.inqool.arclib.service;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.store.AipSipStore;
import cz.inqool.arclib.store.AipXmlStore;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.ConflictObject;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static cz.inqool.uas.util.Utils.notNull;


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
     * @param sipName
     * @param sipHash
     * @param xmlId
     * @param xmlName
     * @param xmlHash
     */
    public void registerAipCreation(String sipId, String sipName, String sipHash, String xmlId, String xmlName, String xmlHash) {
        notNull(sipId, () -> new BadArgument(sipId));
        notNull(xmlId, () -> new BadArgument(xmlId));
        AipSip sip = aipSipStore.find(sipId);
        AipXml xml = aipXmlStore.find(xmlId);
        if (sip != null)
            throw new ConflictObject(sip);
        if (xml != null)
            throw new ConflictObject(xml);
        sip = new AipSip(sipId, sipName, sipHash, AipState.PROCESSING);
        xml = new AipXml(xmlId, xmlName, xmlHash, sip, 1, true);
        aipSipStore.save(sip);
        aipXmlStore.save(xml);
    }

    /**
     * Registers that AIP creation process has ended.
     *
     * @param sipId
     * @param xmlId
     * @throws IllegalStateException if {@link AipSip#state} is not {@link AipState#PROCESSING} or {@link AipXml#processing} is false
     */
    public void finishAipCreation(String sipId, String xmlId) {
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
     * @param xmlId
     * @param xmlName
     * @param xmlHash
     */
    public void registerXmlUpdate(String sipId, String xmlId, String xmlName, String xmlHash) {
        notNull(sipId, () -> new BadArgument(sipId));
        notNull(xmlId, () -> new BadArgument(xmlId));
        AipXml xml = aipXmlStore.find(xmlId);
        if (xml != null)
            throw new ConflictObject(xml);
        aipXmlStore.save(new AipXml(xmlId, xmlName, xmlHash, new AipSip(sipId), aipXmlStore.getNextXmlVersionNumber(sipId), true));
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
     * Logically removes SIP i.e. sets its state to <i>removed</i> in the database.
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

    @Inject
    public void setAipSipStore(AipSipStore store) {
        this.aipSipStore = store;
    }

    @Inject
    public void setAipXmlStore(AipXmlStore store) {
        this.aipXmlStore = store;
    }
}
