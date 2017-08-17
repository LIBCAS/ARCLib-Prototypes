package cas.lib.arclib.domain;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_validation_profile")
public class ValidationProfile extends DatedObject {

    /**
     * XML obsah profilu
     */
    @Column(length = 10485760)
    private String xml;
}
