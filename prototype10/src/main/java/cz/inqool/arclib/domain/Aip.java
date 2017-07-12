package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "arclib_aip")
public class Aip extends ArchivalObject {

    @OneToMany
    @JoinColumn(name = "arclib_aip_id")
    private Set<AipXml> xml = new HashSet<>();
    ;

    @Enumerated(EnumType.STRING)
    private AipState state;
}
