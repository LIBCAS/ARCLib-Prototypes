package cz.inqool.uas.index;

import cz.inqool.uas.domain.DatedObject;
import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.index.dto.Filter;
import cz.inqool.uas.index.dto.FilterOperation;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.store.DomainStore;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static cz.inqool.uas.util.Utils.*;
import static cz.inqool.uas.util.Utils.normalize;

/**
 * {@link cz.inqool.uas.store.DatedStore} with automatic Elasticsearch indexing and filtering.
 *
 * <p>
 *     First purpose of this extension is to hook into {@link DomainStore#save(DomainObject)} method and using defined
 *     {@link IndexedStore#toIndexObject(DomainObject)} method automatically construct Elasticsearch entity from
 *     JPA entity and sending it into Elasticsearch.
 * </p>
 * <p>
 *     Second purpose is retrieval of instances based on complex {@link Params} which encompass filtering, sorting and
 *     paging.
 * </p>
 * <p>
 *     {@link IndexedStore} works only on entities extending {@link DatedObject}.
 * </p>
 *
 * @param <T> type of JPA entity
 * @param <U> type of corresponding Elasticsearch entity
 */
@SuppressWarnings("WeakerAccess")
public interface IndexedStore<T extends DomainObject, U extends IndexedDomainObject> {
    Collection<T> findAll();

    List<T> findAllInList(List<String> ids);

    ElasticsearchTemplate getTemplate();

    Class<U> getUType();

    U toIndexObject(T obj);

    default T save(T entity) {
        return index(entity);
    }

    default void save(Collection<? extends T> entities) {
        index(entities);
    }

    default void delete(T entity) {
        removeIndex(entity);
    }

    /**
     * Reindexes all entities from JPA to Elasticsearch.
     *
     * <p>
     *     Also creates the mapping for type.
     * </p>
     * <p>
     *     This method should be used only if the index was previously deleted and recreated. Does not remove old
     *     mapping and instances from Elasticsearch.
     * </p>
     */
    default void reindex() {
        getTemplate().putMapping(getUType());
        Collection<T> instances = findAll();
        instances.forEach(this::index);
    }

    /**
     * Finds all instances that respect the selected {@link Params}.
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
    default Result<T> findAll(Params params) {
        notNull(params.getSort(), () -> new BadArgument("sort"));
        notNull(params.getOrder(), () -> new BadArgument("order"));
        notNull(params.getPage(), () -> new BadArgument("page"));
        notNull(params.getPageSize(), () -> new BadArgument("pageSize"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withTypes(getIndexType())
                .withFilter(buildFilters(params))
                .withSort(SortBuilders.fieldSort(params.getSort())
                                .order(SortOrder.valueOf(params.getOrder().toString()))
                )
                .withFields("id")
                .withPageable(new PageRequest(params.getPage(), params.getPageSize()))
                .build();

        return getTemplate().query(query, response -> {
            Result<T> result = new Result<>();

            List<String> ids = StreamSupport.stream(response.getHits().spliterator(), true)
                    .map(hit -> hit.field("id").<String>getValue())
                    .collect(Collectors.toList());

            List<T> sorted = findAllInList(ids);

            result.setItems(sorted);
            result.setCount(response.getHits().totalHits());

            return result;
        });
    }

    /**
     * Counts all instances that respect the selected {@link Params}.
     *
     * <p>
     *     Though {@link Params} one could specify filtering. For further explanation
     *     see {@link Params}.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Total number of instances
     */
    default Long count(Params params) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withTypes(getIndexType())
                .withFilter(buildFilters(params))
                .withSort(SortBuilders.fieldSort(params.getSort())
                                      .order(SortOrder.valueOf(params.getOrder().toString()))
                )
                .build();

        return getTemplate().query(query, response -> response.getHits().totalHits());
    }

    /**
     * Builds an IN query.
     *
     * <p>
     *     Tests if the attribute of an instance is found in provided {@link Set} of values. If the {@link Set}
     *     is empty, then this query is silently ignored.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param values {@link Set} of valid values
     * @return Elasticsearch query builder
     */
    default QueryBuilder inQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return QueryBuilders.termsQuery(name, values.toArray());
        } else {
            return nopQuery();
        }
    }

    /**
     * Builds a NOT IN query.
     *
     * <p>
     *     Tests if the attribute of an instance is not found in provided {@link Set} of values. If the {@link Set}
     *     is empty, then this query is silently ignored.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param values {@link Set} of invalid values
     * @return Elasticsearch query builder
     */
    default QueryBuilder notInQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.boolQuery().should(inQuery(name, values)));
        } else {
            return nopQuery();
        }
    }

    /**
     * Builds a string prefix query.
     *
     * <p>
     *     Tests if the attribute of an instance starts with the specified value.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder prefixQuery(String name, String value) {
        return QueryBuilders.prefixQuery(name, value);
    }

    /**
     * Builds a string suffix query.
     *
     * <p>
     *     Tests if the attribute of an instance ends with the specified value.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     *     This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     *     possible.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder suffixQuery(String name, String value) {
        return QueryBuilders.regexpQuery(name, ".*" + value);
    }

    /**
     * Builds a string contains query.
     *
     * <p>
     *     Tests if the attribute of an instance contains the specified value.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     *     This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     *     possible.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder containsQuery(String name, String value) {
        return QueryBuilders.regexpQuery(name, ".*" + value + ".*");
    }

    /**
     * Builds a greater than query.
     *
     * <p>
     *     Tests if the attribute of an instance is greater than the specified value. Applicable to number and date
     *     attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder gtQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).gt(value);
    }

    /**
     * Builds a less than query.
     *
     * <p>
     *     Tests if the attribute of an instance is less than the specified value. Applicable to number and date
     *     attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder ltQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).lt(value);
    }

    /**
     * Builds a greater than or equal query.
     *
     * <p>
     *     Tests if the attribute of an instance is greater than or equal to the specified value. Applicable to number
     *     and date attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder gteQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).gte(value);
    }

    /**
     * Builds a less than or equal query.
     *
     * <p>
     *     Tests if the attribute of an instance is less than or equal to the specified value. Applicable to number
     *     and date attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder lteQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).lte(value);
    }

    /**
     * Builds a dummy query, which does nothing.
     *
     * <p>
     *     Should be used instead of conditionally skipping query building.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @return Elasticsearch query builder
     */
    default QueryBuilder nopQuery() {
        return QueryBuilders.boolQuery();
    }

    /**
     * Builds set query.
     *
     * <p>
     *     Tests if the attribute of an instance is set.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Elasticsearch query builder
     */
    default QueryBuilder notNullQuery(String name) {
        return QueryBuilders.existsQuery(name);
    }


    default QueryBuilder nestedQuery(String name, List<Filter> filters) {
        return QueryBuilders.nestedQuery(name, andQuery(filters));
    }

    default QueryBuilder negateQuery(List<Filter> filters) {
        return QueryBuilders.boolQuery().mustNot(andQuery(filters));
    }
    /**
     * Builds not set query.
     *
     * <p>
     *     Tests if the attribute of an instance is not set.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Elasticsearch query builder
     */
    default QueryBuilder isNullQuery(String name) {
        return QueryBuilders.boolQuery().mustNot(notNullQuery(name));
    }

    /**
     * Builds an OR query between sub-filters.
     *
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Elasticsearch query builder
     */
    default QueryBuilder orQuery(List<Filter> filters) {
        List<QueryBuilder> builders = filters.stream()
                                             .map(this::buildFilter)
                                             .collect(Collectors.toList());

        return orQueryInternal(builders);
    }

    /**
     * Builds an AND query between sub-filters.
     *
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Elasticsearch query builder
     */
    default QueryBuilder andQuery(List<Filter> filters) {
        List<QueryBuilder> builders = filters.stream()
                                             .map(this::buildFilter)
                                             .collect(Collectors.toList());

        return andQueryInternal(builders);
    }

    /**
     * Gets Elasticsearch type
     *
     * @return Name of Elasticsearch type
     */
    default String getIndexType() {
        Document document = getUType().getAnnotation(Document.class);

        if (document != null) {
            return document.type();
        } else {
            throw new GeneralException("Missing Elasticsearch @Document.type for " + getUType().getSimpleName());
        }
    }

    default void removeIndex(T obj) {
        getTemplate().delete(getUType(), obj.getId());
    }

    default T index(T obj) {
        IndexQuery query = new IndexQuery();
        query.setId(obj.getId());
        query.setObject(this.toIndexObject(obj));

        getTemplate().index(query);
        getTemplate().refresh(getUType());

        return obj;
    }

    default void index(Collection<? extends T> objects) {
        if (objects.isEmpty()) {
            return;
        }

        List<IndexQuery> queries = objects.stream()
                .map(obj -> {
                    IndexQuery query = new IndexQuery();
                    query.setId(obj.getId());

                    query.setObject(this.toIndexObject(obj));

                    return query;
                })
                .collect(Collectors.toList());

        getTemplate().bulkIndex(queries);
        getTemplate().refresh(getUType());
    }

    default String sanitizeFilterValue(String value) {
        if (value != null && value.trim().length() > 0) {
            return value.trim();
        } else {
            return null;
        }
    }

    default QueryBuilder buildFilter(Filter filter) {
        String value = sanitizeFilterValue(filter.getValue());
        FilterOperation operation = filter.getOperation();

        if (operation == null) {
            throw new BadArgument("operation");
        }

        if (value == null
                && operation != FilterOperation.AND
                && operation != FilterOperation.OR
                && operation != FilterOperation.NOT_NULL
                && operation != FilterOperation.IS_NULL
                && operation != FilterOperation.NESTED
                && operation != FilterOperation.NEGATE) {
            throw new BadArgument("value");
        }

        String normalizedValue = normalize(value);

        switch (operation) {
            case EQ:
            default:
                return inQuery(filter.getField(), asSet(normalizedValue));
            case NEQ:
                return notInQuery(filter.getField(), asSet(normalizedValue));
            case STARTWITH:
                return prefixQuery(filter.getField(), normalizedValue);
            case ENDWITH:
                return suffixQuery(filter.getField(), normalizedValue);
            case CONTAINS:
                return containsQuery(filter.getField(), normalizedValue);
            case GT:
                return gtQuery(filter.getField(), value);
            case LT:
                return ltQuery(filter.getField(), value);
            case GTE:
                return gteQuery(filter.getField(), value);
            case LTE:
                return lteQuery(filter.getField(), value);
            case AND:
                return andQuery(filter.getFilter());
            case OR:
                return orQuery(filter.getFilter());
            case IS_NULL:
                return isNullQuery(filter.getField());
            case NOT_NULL:
                return notNullQuery(filter.getField());
            case NESTED:
                return nestedQuery(filter.getField(), filter.getFilter());
            case NEGATE:
                return negateQuery(filter.getFilter());
        }
    }

    default QueryBuilder buildFilters(Params params) {
        if (params.getFilter() == null) {
            return nopQuery();
        }

        List<QueryBuilder> queries = params.getFilter().stream()
                .map(this::buildFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        QueryBuilder internalFilter = params.getInternalFilter();
        if (internalFilter != null) {
            return andQueryInternal(asList(queries, internalFilter));
        } else {
            return andQueryInternal(queries);
        }


    }

    default QueryBuilder andQueryInternal(List<QueryBuilder> filters) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        for (QueryBuilder filter : filters) {
            query.must(filter);
        }

        return query;
    }

    default QueryBuilder orQueryInternal(List<QueryBuilder> filters) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        for (QueryBuilder filter : filters) {
            query.should(filter);
        }

        return query;
    }
}