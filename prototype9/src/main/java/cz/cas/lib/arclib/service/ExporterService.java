package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

@Slf4j
@Service
public class ExporterService {

    private DataSource ds;

    public void export(String template, ExportFormat format, OutputStream os) throws IOException {
        JRPdfExporter exporter = new JRPdfExporter();
        JasperPrint jasperPrint = null;
        try{
        JasperReport jasperReport = JasperCompileManager.compileReport(IOUtils.toInputStream(template, "UTF-8"));
        jasperPrint = JasperFillManager.fillReport(jasperReport, null, ds.getConnection());} catch (SQLException e) {
            throw new GeneralException("Error occurred during database access.", e);
        } catch (JRException e) {
            throw new GeneralException("Error occurred during report template compilation. Template:" + System.lineSeparator() + template, e);
        }

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));

        switch (format) {
            case PDF:
                configPDF(exporter);
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format");
        }
        try {
            exporter.exportReport();
        } catch (JRException e) {
            throw new GeneralException("Export to " + format + " failed." + System.lineSeparator() + "Template:" + System.lineSeparator() + template, e);
        }
    }

    private void configPDF(JRPdfExporter exporter) throws IOException {
        SimplePdfReportConfiguration reportConfig
                = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);

        SimplePdfExporterConfiguration exportConfig
                = new SimplePdfExporterConfiguration();
        exportConfig.setMetadataAuthor("ARCLib Reporting System");
        exportConfig.setEncrypted(true);
        exporter.setConfiguration(reportConfig);
        exporter.setConfiguration(exportConfig);
    }

    private void exportXLS() {

    }

    private void exportCSV() {

    }

    private void exportHTML() {

    }

    @Inject
    public void setDS(DataSource ds) {
        this.ds = ds;
    }
}
