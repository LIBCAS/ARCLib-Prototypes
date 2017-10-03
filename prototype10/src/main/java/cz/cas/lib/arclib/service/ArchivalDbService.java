package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.XmlState;
import cz.cas.lib.arclib.exception.*;
import cz.cas.lib.arclib.store.AipSipStore;
import cz.cas.lib.arclib.store.AipXmlStore;
import cz.cas.lib.arclib.store.Transactional;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

import static cz.cas.lib.arclib.util.Utils.notNull;


/**
 * Class used for communication with Archival Storage database which contains transactional data about Archival Storage packages.
 */
@Service
@Transactional
@Log4j
public class ArchivalDbService {

    private AipSipStore aipSipStore;
    private AipXmlStore aipXmlStore;

    /**
     * Registers that AIP creation process has started. Stores AIP records to database and sets their state to <i>processing</i>.
     *
     * @param sipId
     * @param sipHash
     * @param xmlHash
     * @return generated ID of XML record
     */
    public String registerAipCreation(String sipId, String sipHash, String xmlHash) {
        notNull(sipId, () -> new BadArgument(sipId));
        AipSip sip = aipSipStore.find(sipId);
        if (sip != null)
            throw new ConflictObject(sip);
        sip = new AipSip(sipId, sipHash, AipState.PROCESSING);
        AipXml xml = new AipXml(xmlHash, new AipSip(sipId), 1, XmlState.PROCESSING);
        aipSipStore.save(sip);
        aipXmlStore.save(xml);
        return xml.getId();
    }

    /**
     * Registers that AIP creation process has ended.
     *
     * @param sipId
     * @param xmlId
     */
    public void finishAipCreation(String sipId, String xmlId) {
        AipSip sip = aipSipStore.find(sipId);
        sip.setState(AipState.ARCHIVED);
        aipSipStore.save(sip);
        finishXmlProcess(xmlId);
    }

    /**
     * Registers that AIP XML update process has started.
     *
     * @param sipId
     * @param xmlHash
     * @return created XML entity filled with generated ID and version
     */
    public AipXml registerXmlUpdate(String sipId, String xmlHash) {
        notNull(sipId, () -> new BadArgument(sipId));
        int xmlVersion = aipXmlStore.getNextXmlVersionNumber(sipId);
        AipXml newVersion = new AipXml(xmlHash, new AipSip(sipId), xmlVersion, XmlState.PROCESSING);
        aipXmlStore.save(newVersion);
        return newVersion;
    }

    /**
     * Registers that AIP SIP deletion process has started.
     *
     * @param sipId
     * @throws RollbackedException
     * @throws StillProcessingException
     */
    public void registerSipDeletion(String sipId) throws StillProcessingException, RollbackedException {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> {
            log.warn(String.format("Could not find AIP: %s", sipId));
            return new MissingObject(AipSip.class, sipId);
        });
        if (sip.getState() == AipState.PROCESSING)
            throw new StillProcessingException(sip);
        if (sip.getState() == AipState.ROLLBACKED)
            throw new RollbackedException(sip);
        sip.setState(AipState.PROCESSING);
        aipSipStore.save(sip);
    }

    /**
     * Registers that AIP SIP deletion process has ended.
     *
     * @param sipId
     */
    public void finishSipDeletion(String sipId) {
        AipSip sip = aipSipStore.find(sipId);
        sip.setState(AipState.DELETED);
        aipSipStore.save(sip);
    }

    /**
     * Registers that process which used AIP XML file has ended.
     *
     * @param xmlId
     */
    public void finishXmlProcess(String xmlId) {
        AipXml xml = aipXmlStore.find(xmlId);
        xml.setState(XmlState.ARCHIVED);
        aipXmlStore.save(xml);
    }

    /**
     * Logically removes SIP i.e. sets its state to {@link AipState#REMOVED} in the database.
     *
     * @param sipId
     * @throws DeletedException         if SIP is deleted
     * @throws RollbackedException
     * @throws StillProcessingException
     */
    public void removeSip(String sipId) throws DeletedException, RollbackedException, StillProcessingException {
        AipSip sip = aipSipStore.find(sipId);
        notNull(sip, () -> {
            log.warn(String.format("Could not find AIP: %s", sipId));
            return new MissingObject(AipSip.class, sipId);
        });
        if (sip.getState() == AipState.DELETED)
            throw new DeletedException(sip);
        if (sip.getState() == AipState.ROLLBACKED)
            throw new RollbackedException(sip);
        if (sip.getState() == AipState.PROCESSING)
            throw new StillProcessingException(sip);
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
        notNull(sip, () -> {
            log.warn(String.format("Could not find AIP: %s", sipId));
            return new MissingObject(AipSip.class, sipId);
        });
        return sip;
    }

    /**
     * Rollback SIP and related XML. Used when the AIP creation process fails.
     *
     * @param id
     */
    public void rollbackSip(String id, String xmlId) {
        AipSip sip = aipSipStore.find(id);
        sip.setState(AipState.ROLLBACKED);
        aipSipStore.save(sip);
        rollbackXml(xmlId);
    }

    /**
     * Rollback XML. Used when the XML update process or AIP creation process fails.
     *
     * @param id
     */
    public void rollbackXml(String id) {
        AipXml xml = aipXmlStore.find(id);
        xml.setState(XmlState.ROLLBACKED);
        aipXmlStore.save(xml);
    }

    /**
     * Fill initialized lists passed as parameters with records of files in processing state.
     *
     * @param unfinishedSips
     * @param unfinishedXmls
     */
    public void fillUnfinishedFilesLists(List<AipSip> unfinishedSips, List<AipXml> unfinishedXmls) {
        unfinishedSips.addAll(aipSipStore.findUnfinishedSips());
        unfinishedXmls.addAll(aipXmlStore.findUnfinishedXmls());
    }

    /**
     * Deletes records of files in processing state.
     */
    public void rollbackUnfinishedFilesRecords() {
        aipSipStore.rollbackUnfinishedSipsRecords();
        aipXmlStore.rollbackUnfinishedXmlsRecords();
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
