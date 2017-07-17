package cz.inqool.arclib.domain;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Abstract class for core files of archival storage i.e. AipSip data and AipSip XML.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class ArchivalObject extends DatedObject {
    @Column(updatable = false)
    protected String md5;
    @Column(updatable = false)
    protected String name;
}
