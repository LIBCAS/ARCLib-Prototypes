package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "arclib_aip_sip")
public class AipSip extends ArchivalObject {

    @OneToMany(mappedBy = "sip")
    private List<AipXml> xmls = new ArrayList<>();

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    private AipState state;

    public AipSip() {
    }

    public AipSip(String id) {
        this.id = id;
    }

    public void addXml(AipXml aipXml) {
        xmls.add(aipXml);
    }

    public List<AipXml> getXmls() {
        return Collections.unmodifiableList(this.xmls);
    }

    public AipXml getXml( int i){
        return this.xmls.get(i);
    }
}
