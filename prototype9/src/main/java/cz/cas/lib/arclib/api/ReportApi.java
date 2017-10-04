package cz.cas.lib.arclib.api;


import cz.cas.lib.arclib.domain.Report;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.exception.ExporterException;
import cz.cas.lib.arclib.service.ExportFormat;
import cz.cas.lib.arclib.service.ExporterService;
import cz.cas.lib.arclib.store.ReportStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportApi {

    private ReportStore store;
    private ExporterService exporter;

    @RequestMapping(value = "/{reportId}/{format}", method = RequestMethod.GET)
    public void getReport(@PathVariable("reportId") String reportId, @PathVariable("format") String format, @RequestParam Map<String, String> params, HttpServletResponse response) throws IOException, ExporterException {
        response.setStatus(200);
        switch (format.toUpperCase()) {
            case "PDF":
                response.setContentType("application/pdf");
                break;
            case "CSV":
                response.setContentType("text/csv");
                break;
            case "XLSX":
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "HTML":
                response.setContentType("text/html");
                break;
            default:
                throw new BadArgument("Unsupported export format");
        }
        Report r = store.find(reportId);
        response.addHeader("Content-Disposition", "attachment; filename=" + r.getName() + "_" + LocalDate.now().toString()+"."+format.toLowerCase());
        exporter.export(r, ExportFormat.valueOf(format.toUpperCase()), params, response.getOutputStream());
    }

    @RequestMapping(method = RequestMethod.POST)
    public String save(@RequestParam("template") MultipartFile template, @RequestParam("name") String name) throws IOException {
        try (InputStream is = template.getInputStream()) {
            return store.saveReport(name, template.getInputStream());
        }
    }

    @Inject
    public void setReportStore(ReportStore store) {
        this.store = store;
    }

    @Inject
    public void setExporter(ExporterService exporter) {
        this.exporter = exporter;
    }
}
