package cas.lib.arclib.api;

import cz.inqool.uas.domain.DomainObject;
import cas.lib.arclib.domain.Job;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingObject;
import cas.lib.arclib.store.JobStore;
import cz.inqool.uas.store.Transactional;
import cz.inqool.uas.util.Utils;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

@RestController
@RequestMapping("/api/job")
public class JobApi {
    @Getter
    private JobStore store;

    /**
     * Saves an instance.
     * <p>
     * <p>
     * Specified id should correspond to {@link DomainObject#id} otherwise exception is thrown.
     * </p>
     *
     * @param id      Id of the instance
     * @param request Single instance
     * @return Single instance (possibly with computed attributes)
     * @throws BadArgument if specified id does not correspond to {@link DomainObject#id}
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    Job save(@PathVariable("id") String id, @RequestBody Job request) {
        Utils.eq(id, request.getId(), () -> new BadArgument("id"));

        return store.save(request);
    }

    /**
     * Deletes an instance.
     *
     * @param id Id of the instance
     * @throws MissingObject if specified instance is not found
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    void delete(@PathVariable("id") String id) {
        Job entity = store.find(id);
        Utils.notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);
    }

    /**
     * Gets one instance specified by id.
     *
     * @param id Id of the instance
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    Job get(@PathVariable("id") String id) {
        Job entity = store.find(id);
        Utils.notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    /**
     * Gets all instances
     *
     * @return All instances
     */
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    Collection<Job> list() {
        return store.findAll();
    }

    @Inject
    public void setStore(JobStore store) {
        this.store = store;
    }
}
