package cz.cas.lib.arclib.index;

/**
 * Filter operation to do.
 */
public enum FilterOperation {
    /**
     * Equal.
     */
    EQ,

    /**
     * Not equal.
     */
    NEQ,

    /**
     * Greater than.
     * <p>
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    GT,

    /**
     * Less than.
     * <p>
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    LT,

    /**
     * Greater than or equals.
     * <p>
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    GTE,

    /**
     * Less than or equals.
     * <p>
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    LTE,

    /**
     * Starts with.
     * <p>
     * <p>
     * Applicable to string attributes.
     * </p>
     */
    STARTWITH,

    /**
     * Ends with.
     * <p>
     * <p>
     * Applicable to string attributes.
     * </p>
     */
    ENDWITH,

    /**
     * Contains.
     * <p>
     * <p>
     * Applicable to string attributes.
     * </p>
     */
    CONTAINS,

    /**
     * Logical AND.
     * <p>
     * <p>
     * Applicable to sub-filters.
     * </p>
     */
    AND,

    /**
     * Logical OR.
     * <p>
     * <p>
     * Applicable to sub-filters.
     * </p>
     */
    OR,

    /**
     * Is not set
     * <p>
     * <p>
     * Applicable to all attributes.
     * </p>
     */
    IS_NULL,

    /**
     * Is set
     * <p>
     * <p>
     * Applicable to all attributes.
     * </p>
     */
    NOT_NULL,


    /**
     * Negates inner filter
     */
    NEGATE
};
