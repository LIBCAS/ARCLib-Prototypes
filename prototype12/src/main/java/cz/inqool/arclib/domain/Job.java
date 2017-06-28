package cz.inqool.arclib.domain;

import cz.inqool.arclib.ScriptType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_job")
    public class Job extends DatedObject {
        private String name;

        private String timing;

        @Enumerated(EnumType.STRING)
        private ScriptType scriptType;

        private String script;

        private Boolean active;
}
