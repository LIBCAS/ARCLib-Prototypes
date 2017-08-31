package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Report;
import cz.cas.lib.arclib.store.ReportStore;
import cz.cas.lib.arclib.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRSaver;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class ReportService {
    private ReportStore store;
    private ExporterService exporter;

    @Transactional
    public String saveReport(String name,InputStream template) throws IOException {
        return store.save(new Report(name, IOUtils.toString(template, StandardCharsets.UTF_8))).getId();
    }

    public String report(String templateId, ExportFormat format, OutputStream os) throws IOException {
        Report report = store.find(templateId);
        exporter.export(report.getTemplate(),format,os);
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
