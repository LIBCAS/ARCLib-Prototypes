package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.AipSip;
import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.domain.QAipXml;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.store.DomainStore;
import org.springframework.stereotype.Repository;

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
}
