package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QReport;
import cz.cas.lib.arclib.domain.Report;
import cz.cas.lib.arclib.exception.GeneralException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Repository
public class ReportStore extends DomainStore<Report, QReport> {
    public ReportStore() {
        super(Report.class, QReport.class);
    }

    @Transactional
    public String saveReport(String name, InputStream template) throws IOException {
        byte[] templateBytes = IOUtils.toByteArray(template);
        IOUtils.closeQuietly(template);
        JasperReport compiledReport;
        template = new ByteArrayInputStream(templateBytes);
        try {
            compiledReport = JasperCompileManager.compileReport(template);
        } catch (JRException e) {
            throw new GeneralException("Error occurred during report template compilation.", e);
        }
        template = new ByteArrayInputStream(templateBytes);
        return save(new Report(name, IOUtils.toString(template, StandardCharsets.UTF_8), compiledReport)).getId();
    }
}
