package cz.inqool.arclib.store;

import cz.inqool.arclib.domain.AipXml;
import cz.inqool.arclib.domain.QAipXml;
import cz.inqool.uas.store.DomainStore;
import org.springframework.stereotype.Repository;

@Repository
public class AipXmlStore extends DomainStore<AipXml, QAipXml> {
    public AipXmlStore() {
        super(AipXml.class, QAipXml.class);
    }
}
