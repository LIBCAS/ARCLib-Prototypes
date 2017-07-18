package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "arclib_aip_sip")
public class AipSip extends ArchivalObject {

    @OneToMany(mappedBy = "sip", fetch = FetchType.EAGER)
    private List<AipXml> xmls = new ArrayList<>();

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    private AipState state;

    public void addXml(AipXml aipXml) {
        xmls.add(aipXml);
    }

    public List<AipXml> getXmls() {
        return Collections.unmodifiableList(this.xmls);
    }

    public AipXml getXml(int i) {
        return this.xmls.get(i);
    }

    public AipSip() {
        super(null, null, null);
    }

    public AipSip(String id) {
        super(id, null, null);
    }

    public AipSip(String id, String name, String md5, AipState state) {
        super(id, name, md5);
        this.state = state;
    }

    public AipSip(String id, String name, String md5, AipState state, AipXml... xmls) {
        this(id,name,md5,state);
        for (AipXml xml : xmls) {
            addXml(xml);
        }
    }
}
