package cz.inqool.arclib.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_batch")
public class Batch extends DatedObject {

    @BatchSize(size=100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="arclib_batch_i", joinColumns=@JoinColumn(name="batch_id"))
    @Column(name="id")
    private Set<String> ids = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private BatchState state;
}
