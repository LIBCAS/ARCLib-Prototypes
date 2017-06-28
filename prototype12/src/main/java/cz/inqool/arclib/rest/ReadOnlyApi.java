package cz.inqool.arclib.rest;

import cz.inqool.arclib.domain.DomainObject;
import cz.inqool.arclib.exception.MissingObject;
import cz.inqool.arclib.index.IndexedStore;
import cz.inqool.arclib.index.dto.Params;
import cz.inqool.arclib.index.dto.Result;
import cz.inqool.arclib.rest.data.DataAdapter;
import cz.inqool.arclib.store.Transactional;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import static cz.inqool.arclib.util.Utils.notNull;

/**
 * Generic RESTful CRUD API for accessing {@link IndexedStore}.
 *
 * @param <T> type of JPA entity
 */
public interface ReadOnlyApi<T extends DomainObject> {

    DataAdapter<T> getAdapter();
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
    default T get(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        T entity = getAdapter().find(id);
        notNull(entity, () -> new MissingObject(getAdapter().getType(), id));

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
    default Result<T> list(@ApiParam(value = "Parameters to comply with", required = true)
                           @ModelAttribute Params params) {
        return getAdapter().findAll(params);
    }
}
