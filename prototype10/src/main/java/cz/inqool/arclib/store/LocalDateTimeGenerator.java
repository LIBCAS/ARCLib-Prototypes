package cz.inqool.arclib.store;

import org.hibernate.Session;
import org.hibernate.tuple.ValueGenerator;

import java.time.LocalDateTime;

public class LocalDateTimeGenerator implements ValueGenerator<LocalDateTime> {
    @Override
    public LocalDateTime generateValue(Session session, Object owner) {
        return LocalDateTime.now();
    }
}
