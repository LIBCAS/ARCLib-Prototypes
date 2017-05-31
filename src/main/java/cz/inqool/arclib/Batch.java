package cz.inqool.arclib;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

/**
 * Dávka
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "arclib_batch")
public class Batch {
    @Id
    protected String id = UUID.randomUUID().toString();

    @BatchSize(size=100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="arclib_batch_i", joinColumns=@JoinColumn(name="batch_id"))
    private Set<String> ids;
}
