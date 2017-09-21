package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.AipSip;
import cz.cas.lib.arclib.domain.AipXml;
import cz.cas.lib.arclib.domain.QAipXml;
import cz.cas.lib.arclib.exception.MissingObject;
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
