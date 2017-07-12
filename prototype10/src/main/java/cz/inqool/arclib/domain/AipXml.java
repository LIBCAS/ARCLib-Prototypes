package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_aip_xml")
public class AipXml extends ArchivalObject {
    private int version;
}
