package org.nuxeo.ecm.core.search.client.opensearch1.aggregate;

import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.Aggregate;

/**
 * @since 2025.0
 */
public final class AggregateParserBase {

    public static final String FULLTEXT_FIELD = "all_field";

    public static final String AGG_FILTER_SUFFIX = "_filter";

    public static final char XPATH_SEP = '/';

    public static final char ES_MULTI_LEVEL_SEP = '.';

    public static String getFilterId(Aggregate<?> agg) {
        return agg.getId() + AGG_FILTER_SUFFIX;
    }

    public static String getFieldName(String name) {
        if (NXQL.ECM_FULLTEXT.equals(name)) {
            return FULLTEXT_FIELD;
        }
        return name.replace(XPATH_SEP, ES_MULTI_LEVEL_SEP);
    }
}
