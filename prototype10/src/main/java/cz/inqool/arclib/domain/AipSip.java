package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "arclib_aip")
public class AipSip extends ArchivalObject {

    @OneToMany(mappedBy = "sip")
    private Set<AipXml> xmls = new HashSet<>();

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

    public Set<AipXml> getXmls() {
        return Collections.unmodifiableSet(this.xmls);
    }
}
