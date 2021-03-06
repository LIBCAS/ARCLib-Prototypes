package cz.cas.lib.arclib.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "arclib_aip_sip")
/**
 * SIP database entity. Its id is used in API calls and is projected into storage layer.
 */
public class AipSip extends ArchivalObject {

    @Override
    @JsonIgnore(false)
    @JsonProperty
    public String getId() {
        return super.getId();
    }

    @OneToMany(mappedBy = "sip", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<AipXml> xmls = new ArrayList<>();

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    private AipState state;

    public AipSip() {
        super(null, null);
    }

    public AipSip(String id) {
        super(id, null);
    }

    public AipSip(String id, String md5, AipState state) {
        super(id, md5);
        this.state = state;
    }

    public AipSip(String id, String md5, AipState state, AipXml... xmls) {
        this(id, md5, state);
        for (AipXml xml : xmls) {
            addXml(xml);
        }
    }

    public void addXml(AipXml aipXml) {
        xmls.add(aipXml);
    }

    public List<AipXml> getXmls() {
        return Collections.unmodifiableList(this.xmls);
    }

    public AipXml getXml(int i) {
        return this.xmls.get(i);
    }

    @JsonIgnore
    public AipXml getLatestXml() {
        return this.xmls.stream().max(Comparator.comparingInt(xml -> xml.getVersion())).get();
    }
}
