package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.Instant;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_job")
public class Job extends DatedObject {
    private String name;

    private String timing;

    private String script;

    private Boolean active;

    private int lastReturnCode;

    private Instant lastExecutionTime;
}
