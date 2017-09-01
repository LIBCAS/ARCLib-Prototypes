package cz.cas.lib.arclib.api;


import cz.cas.lib.arclib.exception.ExporterException;
import cz.cas.lib.arclib.service.ExportFormat;
import cz.cas.lib.arclib.service.ReportService;
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

    private ReportService service;

    @RequestMapping(value = "/{reportId}/{format}", method = RequestMethod.GET)
    public void getReport(@PathVariable("reportId") String reportId, @PathVariable("format") ExportFormat format, @RequestParam Map<String, String> params, HttpServletResponse response) throws IOException, ExporterException {
        response.setStatus(200);
        switch (format) {
            case PDF:
                response.setContentType("application/pdf");
                break;
            case CSV:
                response.setContentType("text/csv");
                break;
            case XLS:
                response.setContentType("application/vnd.ms-excel");
                break;
            case HTML:
                response.setContentType("text/html");
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format");
        }
        String reportName = service.report(reportId, format, params, response.getOutputStream());
        response.addHeader("Content-Disposition", "attachment; filename=" + reportName + "_" + LocalDate.now().toString());
    }

    @RequestMapping(method = RequestMethod.POST)
    public String save(@RequestParam("template") MultipartFile template, @RequestParam("name") String name) throws IOException {
        try (InputStream is = template.getInputStream()) {
            return service.saveReport(name, template.getInputStream());
        }
    }

    @Inject
    public void setReportService(ReportService service) {
        this.service = service;
    }

}
