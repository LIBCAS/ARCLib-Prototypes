package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.ValidationProfile;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.domain.general.DomainObject;
import cz.cas.lib.arclib.exception.general.BadArgument;
import cz.cas.lib.arclib.exception.general.MissingObject;
import cz.cas.lib.arclib.store.general.Transactional;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.arclib.Utils.eq;
import static cz.cas.lib.arclib.Utils.notNull;

@RestController
@RequestMapping("/api/validation_profile")
public class ValidationProfileApi {
    @Getter
    private ValidationProfileStore store;

    /**
     * Saves an instance.
     *
     * <p>
     *     Specified id should correspond to {@link DomainObject#id} otherwise exception is thrown.
     * </p>
     * @param id Id of the instance
     * @param request Single instance
     * @return Single instance (possibly with computed attributes)
     * @throws BadArgument if specified id does not correspond to {@link DomainObject#id}
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    ValidationProfile save(@PathVariable("id") String id, @RequestBody ValidationProfile request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

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
        ValidationProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

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
    ValidationProfile get(@PathVariable("id") String id) {
        ValidationProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    /**
     * Gets all instances
     *
     * @return All instances
     */
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    Collection<ValidationProfile> list() {
        return store.findAll();
    }

    @Inject
    public void setStore(ValidationProfileStore store) {
        this.store = store;
    }
}
