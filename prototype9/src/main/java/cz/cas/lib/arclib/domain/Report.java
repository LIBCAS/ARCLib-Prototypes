package cz.cas.lib.arclib.domain;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.SerializationUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "arclib_report")
@Setter
@Getter
public class Report extends DomainObject implements Serializable {
    @Column(unique = true)
    private String name;
    private String template;
    private byte[] compiled;

    public Report() {
    }

    public Report(String name, String template, Object compiled) {
        this.name = name;
        this.template = template;
        this.setCompiledObject(compiled);
    }

    public Object getCompiledObject() {
        return SerializationUtils.deserialize(compiled);
    }

    public void setCompiledObject(Object compiled) {
        this.compiled = SerializationUtils.serialize((Serializable) compiled);
    }
}
