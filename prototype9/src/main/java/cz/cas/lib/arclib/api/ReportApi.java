package cz.cas.lib.arclib.api;


import cz.cas.lib.arclib.domain.Report;
import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.service.ExportFormat;
import cz.cas.lib.arclib.service.ExporterService;
import cz.cas.lib.arclib.store.ReportStore;
import lombok.extern.log4j.Log4j;
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
@Log4j
public class ReportApi {

    private ReportStore store;
    private ExporterService exporter;

    /**
     * Find report template by ID and exports it to specified format.
     * @param reportId
     * @param format   output format, one of CSV, XLSX, HTML, PDF
     * @param params   Query string with parameters used to override default parameters values which are specified inside
     *                 template itself. If given parameter is not defined within template or the value can not be parsed
     *                 API returns BAD_REQUEST (400). Boolean parameter value should be in string form i.e. true/false
     *
     */
    @RequestMapping(value = "/{reportId}/{format}", method = RequestMethod.GET)
    public void getReport(@PathVariable("reportId") String reportId, @PathVariable("format") String format, @RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
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
                String e = String.format("Unsupported export format: %s", format);
                log.warn(e);
                throw new BadArgument(e);
        }
        Report r = store.find(reportId);
        response.addHeader("Content-Disposition", "attachment; filename=" + r.getName() + "_" + LocalDate.now().toString() + "." + format.toLowerCase());
        exporter.export(r, ExportFormat.valueOf(format.toUpperCase()), params, response.getOutputStream());
    }

    /**
     * Compiles and stores report template.
     * <p>
     * Validate type of template custom parameters and their default values if there are any. If parameters validation
     * fails returns BAD_REQUEST (400).
     * </p>
     * Supported parameter types:
     * <ul>
     * <li>java.lang.String</li>
     * <li>java.lang.Short</li>
     * <li>java.lang.Long</li>
     * <li>java.lang.Integer</li>
     * <li>java.lang.Float</li>
     * <li>java.lang.Double</li>
     * <li>java.lang.Boolean</li>
     * </ul>
     * Boolean parameter value should be in string form i.e. true/false
     * @param name     template name
     * @param template template file
     * @return
     */
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
