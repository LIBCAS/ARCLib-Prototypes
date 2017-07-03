package cz.inqool.arclib.api;

import cz.inqool.arclib.service.CoordinatorService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/coordinator")
public class CoordinatorApi {

    private CoordinatorService coordinatorService;

    /**
     * Starts processing of SIPs stored in the specified folder
     * @param path path to the folder containing SIPs
     */
    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public void start(@RequestBody String path) {
        coordinatorService.run(path);
    }

    /**
     * Suspends processing of the batch
     * @param batchId id of the batch to suspend
     */
    @RequestMapping(value = "/{batchId}/suspend", method = RequestMethod.POST)
    public void suspend(@PathVariable("batchId") String batchId) {
        coordinatorService.suspend(batchId);
    }

    /**
     * Cancels processing of the batch
     * @param batchId id of the batch to cancel
     */
    @RequestMapping(value = "/{batchId}/cancel", method = RequestMethod.POST)
    public void cancel(@PathVariable("batchId") String batchId) {
        coordinatorService.cancel(batchId);
    }

    /**
     * Resumes processing of the batch
     * @param batchId id of the batch to resume
     */
    @RequestMapping(value = "/{batchId}/resume", method = RequestMethod.POST)
    public void resume(@PathVariable("batchId") String batchId) {
        coordinatorService.resume(batchId);
    }

    @Inject
    public void setCoordinatorService(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }
}
