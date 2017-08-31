package cz.cas.lib.arclib.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "arclib_report")
@Setter
@Getter
@AllArgsConstructor
public class Report extends DomainObject{
    @Column(unique = true)
    private String name;
    private String template;

    public Report(){}

}
