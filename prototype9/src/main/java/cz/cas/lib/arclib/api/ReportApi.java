package cz.cas.lib.arclib.api;


import cz.cas.lib.arclib.service.ExportFormat;
import cz.cas.lib.arclib.service.ReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/report")
public class ReportApi {

    private ReportService service;

    @RequestMapping(value = "/{reportId}", method = RequestMethod.GET)
    public void getReport(@PathVariable("reportId") String reportId, @RequestParam("format") ExportFormat format, HttpServletResponse response) throws IOException {
//        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
//        Instant.from(timeFormatter.parse(dateFrom));
//        Instant.from(timeFormatter.parse(dateTo));
        //@RequestParam("dateFrom") String dateFrom, @RequestParam("dateTo") String dateTo,

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
        response.addHeader("Content-Disposition", "attachment; filename=" + service.report(reportId, format, response.getOutputStream()) + "_" + LocalDate.now().toString());
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
