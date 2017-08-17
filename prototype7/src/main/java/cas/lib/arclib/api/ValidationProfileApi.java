package cas.lib.arclib.api;

import cz.inqool.uas.domain.DomainObject;
import cas.lib.arclib.domain.ValidationProfile;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.store.Transactional;
import cas.lib.arclib.store.ValidationProfileStore;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

import static cz.inqool.uas.util.Utils.eq;
import static cz.inqool.uas.util.Utils.notNull;

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

    @Inject
    public void setStore(ValidationProfileStore store) {
        this.store = store;
    }
}
