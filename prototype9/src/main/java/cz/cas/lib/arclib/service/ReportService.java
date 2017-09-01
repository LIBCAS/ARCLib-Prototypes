package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Report;
import cz.cas.lib.arclib.exception.ExporterException;
import cz.cas.lib.arclib.exception.GeneralException;
import cz.cas.lib.arclib.store.ReportStore;
import cz.cas.lib.arclib.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
public class ReportService {
    private ReportStore store;
    private ExporterService exporter;

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
        return store.save(new Report(name, IOUtils.toString(template, StandardCharsets.UTF_8), compiledReport)).getId();
    }

    @Transactional
    public String report(String templateId, ExportFormat format, Map<String, String> customParams, OutputStream os) throws IOException, ExporterException {
        Report report = store.find(templateId);
        exporter.export(report, format, customParams, os);
        return report.getName();
    }

    @Inject
    public void setAipSipStore(ReportStore store) {
        this.store = store;
    }

    @Inject
    public void setExporter(ExporterService exporter) {
        this.exporter = exporter;
    }
}
