package com.data.pivot.plugin.mapping;

public enum MappingConfidence {
    /** Exact name match after preprocess (case/underscore insensitive). */
    EXACT,
    /** Settings strategy produced a table/column name that exists in the scoped catalog. */
    NAMING,
    /** Unique Jaro-Winkler hit at or above 0.9 when Settings is unconfigured. */
    FUZZY,
    /** Two or more equally plausible hits; do not pick a winner. */
    AMBIGUOUS,
    UNRESOLVED
}
