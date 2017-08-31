package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QReportSource1;
import cz.cas.lib.arclib.domain.ReportSource1;
import org.springframework.stereotype.Repository;

@Repository
public class ReportSource1Store extends DatedStore<ReportSource1, QReportSource1> {
    public ReportSource1Store() {
        super(ReportSource1.class, QReportSource1.class);
    }
}
