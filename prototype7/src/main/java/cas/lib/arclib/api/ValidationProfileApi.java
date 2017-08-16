package cas.lib.arclib.api;

import cas.lib.arclib.domain.DomainObject;
import cas.lib.arclib.domain.ValidationProfile;
import cas.lib.arclib.exception.BadArgument;
import cas.lib.arclib.exception.MissingObject;
import cas.lib.arclib.store.Transactional;
import cas.lib.arclib.store.ValidationProfileStore;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

import static cas.lib.arclib.util.Utils.eq;
import static cas.lib.arclib.util.Utils.notNull;

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
    @ApiOperation(value = "Saves an instance", notes = "Returns single instance (possibly with computed attributes)",
            response = DomainObject.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = DomainObject.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    ValidationProfile save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                                   @ApiParam(value = "Single instance", required = true)
                   @RequestBody ValidationProfile request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        return store.save(request);
    }

    /**
     * Deletes an instance.
     *
     * @param id Id of the instance
     * @throws MissingObject if specified instance is not found
     */
    @ApiOperation(value = "Deletes an instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    void delete(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
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
    @ApiOperation(value = "Gets one instance specified by id", response = DomainObject.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = DomainObject.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    ValidationProfile get(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        ValidationProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    /**
     * Gets all instances that respect the selected {@link Params}.
     *
     * <p>
     *     Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     *     see {@link Params}.
     * </p>
     * <p>
     *     Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    @ApiOperation(value = "Gets all instances that respect the selected parameters",
            notes = "Returns sorted list of instances with total number", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    Result<ValidationProfile> list(@ApiParam(value = "Parameters to comply with", required = true)
                           @ModelAttribute Params params) {
        return store.findAll(params);
    }

    @Inject
    public void setStore(ValidationProfileStore store) {
        this.store = store;
    }
}
