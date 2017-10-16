package cz.cas.lib.arclib.solr;

import cz.cas.lib.arclib.exception.BadArgument;
import cz.cas.lib.arclib.index.Filter;
import cz.cas.lib.arclib.index.FilterOperation;
import org.springframework.data.solr.core.query.Criteria;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.Utils.asSet;
import static cz.cas.lib.arclib.Utils.normalize;

public class SolrQueryBuilder {

    public static Criteria buildFilters(List<Filter> filters) {
        List<Criteria> queries = filters.stream()
                .map(SolrQueryBuilder::buildFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return andQueryInternal(queries);
    }

    public static Criteria buildFilter(Filter filter) {
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
            case NEGATE:
                return negateQuery(filter.getFilter());
        }
    }

    public static String sanitizeFilterValue(String value) {
        if (value != null && value.trim().length() > 0) {
            return value.trim();
        } else {
            return null;
        }
    }

    public static Criteria andQueryInternal(List<Criteria> filters) {
        return filters.stream()
                .reduce(nopQuery(), Criteria::and);
    }

    /**
     * Builds an IN query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is found in provided {@link Set} of values. If the {@link Set}
     * is empty, then this query is silently ignored.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name   Name of the attribute to check
     * @param values {@link Set} of valid values
     * @return Solr query builder
     */
    public static Criteria inQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return Criteria.where(name).in(values);
        } else {
            return nopQuery();
        }
    }

    /**
     * Builds a NOT IN query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is not found in provided {@link Set} of values. If the {@link Set}
     * is empty, then this query is silently ignored.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name   Name of the attribute to check
     * @param values {@link Set} of invalid values
     * @return Solr query builder
     */
    public static Criteria notInQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return Criteria.where(name).in(values).notOperator();
        } else {
            return nopQuery();
        }
    }

    /**
     * Builds a string prefix query.
     * <p>
     * <p>
     * Tests if the attribute of an instance starts with the specified value.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria prefixQuery(String name, String value) {
        return Criteria.where(name).startsWith(value);
    }

    /**
     * Builds a string suffix query.
     * <p>
     * <p>
     * Tests if the attribute of an instance ends with the specified value.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     * This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     * possible.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria suffixQuery(String name, String value) {
        return Criteria.where(name).endsWith(value);
    }

    /**
     * Builds a string contains query.
     * <p>
     * <p>
     * Tests if the attribute of an instance contains the specified value.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     * This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     * possible.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria containsQuery(String name, String value) {
        return Criteria.where(name).contains(value);
    }

    /**
     * Builds a greater than query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is greater than the specified value. Applicable to number and date
     * attributes.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria gtQuery(String name, String value) {
        return Criteria.where(name).greaterThan(value);
    }

    /**
     * Builds a less than query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is less than the specified value. Applicable to number and date
     * attributes.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria ltQuery(String name, String value) {
        return Criteria.where(name).lessThan(value);
    }

    /**
     * Builds a greater than or equal query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is greater than or equal to the specified value. Applicable to number
     * and date attributes.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria gteQuery(String name, String value) {
        return Criteria.where(name).greaterThanEqual(value);
    }

    /**
     * Builds a less than or equal query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is less than or equal to the specified value. Applicable to number
     * and date attributes.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria lteQuery(String name, String value) {
        return Criteria.where(name).lessThanEqual(value);
    }

    /**
     * Builds a dummy query, which does nothing.
     * <p>
     * <p>
     * Should be used instead of conditionally skipping query building.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @return Solr query builder
     */
    public static Criteria nopQuery() {
        return Criteria.where("id");
    }

    /**
     * Builds set query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is set.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query builder
     */
    public static Criteria notNullQuery(String name) {
        return Criteria.where(name).isNotNull();
    }

    public static Criteria negateQuery(List<Filter> filters) {
        return andQuery(filters).notOperator();
    }

    /**
     * Builds not set query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is not set.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query builder
     */
    public static Criteria isNullQuery(String name) {
        return Criteria.where(name).isNull();
    }

    /**
     * Builds an OR query between sub-filters.
     * <p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    public static Criteria orQuery(List<Filter> filters) {
        List<Criteria> builders = filters.stream()
                .map(SolrQueryBuilder::buildFilter)
                .collect(Collectors.toList());

        return orQueryInternal(builders);
    }

    /**
     * Builds an AND query between sub-filters.
     * <p>
     * <p>
     * Used internally in {@link SolrStore#findAll(List<Filter>)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    public static Criteria andQuery(List<Filter> filters) {
        List<Criteria> builders = filters.stream()
                .map(SolrQueryBuilder::buildFilter)
                .collect(Collectors.toList());

        return andQueryInternal(builders);
    }

    public static Criteria orQueryInternal(List<Criteria> filters) {
        return filters.stream()
                .reduce(nopQuery(), Criteria::or);
    }
}
