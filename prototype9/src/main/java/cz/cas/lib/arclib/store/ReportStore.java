package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QReport;
import cz.cas.lib.arclib.domain.Report;
import org.springframework.stereotype.Repository;

@Repository
public class ReportStore extends DomainStore<Report, QReport> {
    public ReportStore() {
        super(Report.class, QReport.class);
    }
}
