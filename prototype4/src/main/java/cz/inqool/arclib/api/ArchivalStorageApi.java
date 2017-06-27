package cz.inqool.arclib.api;

import cz.inqool.arclib.domain.AipState;
import cz.inqool.arclib.service.FixityService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/archival")
public class ArchivalStorageApi {

    private FixityService fixityService;

    @RequestMapping(value = "/state", method = RequestMethod.GET)
    public AipState start(@RequestParam String path) throws IOException, NoSuchAlgorithmException {
        return fixityService.getAipState(Paths.get(path));
    }

    @Inject
    public void setFixityService(FixityService fixityService) {
        this.fixityService = fixityService;
    }
}
