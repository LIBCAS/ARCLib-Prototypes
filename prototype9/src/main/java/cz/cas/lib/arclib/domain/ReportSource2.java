package cz.cas.lib.arclib.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "arclib_report_source_2")
@Setter
@Getter
@AllArgsConstructor
public class ReportSource2 extends DatedObject{
    Long source2data;
}
