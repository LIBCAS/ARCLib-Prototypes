package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.domain.QAipXml;
import cz.inqool.uas.store.DomainStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AipXmlStore extends DomainStore<AipXml, QAipXml> {
    public AipXmlStore() {
        super(AipXml.class, QAipXml.class);
    }

    public int getNextXmlVersionNumber(String sipId) {
        QAipXml xml = qObject();
        return 1 + query().select(xml.version.max()).where(xml.sip.id.eq(sipId)).fetchFirst();
    }

    public List<String> getXmlIds(String sipId) {
        QAipXml xml = qObject();
        return query().select(xml.id).where(xml.sip.id.eq(sipId)).fetch();
    }
}
