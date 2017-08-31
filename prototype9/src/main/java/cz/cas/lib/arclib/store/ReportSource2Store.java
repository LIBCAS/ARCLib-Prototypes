package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QReportSource2;
import cz.cas.lib.arclib.domain.ReportSource2;
import org.springframework.stereotype.Repository;

@Repository
public class ReportSource2Store extends DatedStore<ReportSource2, QReportSource2> {
    public ReportSource2Store() {
        super(ReportSource2.class, QReportSource2.class);
    }
}