package cz.inqool.arclib;

import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/coordinator")
public class CoordinatorApi {

    private Coordinator coordinator;

    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public void start(@RequestBody Batch batch) {
        coordinator.run(batch);
    }

    @RequestMapping(value = "/{batchId}/suspend", method = RequestMethod.POST)
    public void suspend(@PathVariable("batchId") String batchId) {
        coordinator.suspend(batchId);
    }

    @RequestMapping(value = "/{batchId}/cancel", method = RequestMethod.POST)
    public void cancel(@PathVariable("batchId") String batchId) {
        coordinator.cancel(batchId);
    }

    @Inject
    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }
}
