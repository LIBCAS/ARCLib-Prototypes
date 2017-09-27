package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipState;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.QAipXml;
import cz.cas.lib.arclib.exception.MissingObject;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AipXmlStore extends DomainStore<AipXml, QAipXml> {
    public AipXmlStore() {
        super(AipXml.class, QAipXml.class);
    }

    public int getNextXmlVersionNumber(String sipId) {
        QAipXml xml = qObject();
        Integer lastVersion = query().select(xml.version.max()).where(xml.sip.id.eq(sipId)).fetchFirst();
        if (lastVersion == null)
            throw new MissingObject(AipSip.class, sipId);
        return 1 + lastVersion;
    }

    /**
     * @return all XMLs in processing state which SIP is not in processing state
     */
    public List<AipXml> findUnfinishedXmls() {
        QAipXml xml = qObject();
        return (List<AipXml>) query().where(xml.processing.eq(true)).where(xml.sip.state.eq(AipState.PROCESSING).not()).fetch();
    }

    public void deleteUnfinishedXmlsRecords() {
        QAipXml xml = qObject();
        queryFactory.delete(xml)
                .where(xml.processing.eq(true))
                .execute();
    }
}
