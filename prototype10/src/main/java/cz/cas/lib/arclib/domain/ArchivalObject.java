package cz.cas.lib.arclib.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.store.InstantGenerator;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.time.Instant;

/**
 * Abstract class for core files of archival storage i.e. AipSip data and AipSip XML.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class ArchivalObject extends DomainObject {
    @Column(updatable = false, nullable = false)
    @JsonIgnore
    protected String md5;
    @Column(updatable = false, nullable = false)
    protected String name;
    @Column(updatable = false)
    @GeneratorType(type = InstantGenerator.class, when = GenerationTime.INSERT)
    protected Instant created;
    @Transient
    boolean consistent;

    public ArchivalObject(String id, String name, String md5) {
        this.id = id;
        this.md5 = md5;
        this.name = name;
    }
}
