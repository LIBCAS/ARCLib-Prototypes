package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_aip_xml")
public class AipXml extends ArchivalObject {

    @ManyToOne
    @JoinColumn(name = "arclib_aip_sip_id")
    private AipSip sip;
    private int version;
    private boolean processing;

    public AipXml() {
        super(null, null, null);
    }

    public AipXml(String id, String name, String md5, AipSip sip, int version, boolean processing) {
        super(id, name, md5);
        this.sip = sip;
        this.version = version;
        this.processing = processing;
    }
}
