/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.query.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.apache.commons.collections4.CollectionUtils;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * @since 2025.0
 */
public class PageProviderDefinitionBuilder {

    protected String name;

    protected boolean enabled;

    protected Map<String, String> properties;

    protected String[] queryParameters;

    protected boolean quotePatternParameters;

    protected boolean escapePatternParameters;

    protected String pattern;

    protected WhereClauseDefinition whereClause;

    protected String searchDocumentType;

    protected boolean sortable;

    protected List<SortInfo> sortInfos;

    protected String sortInfosBinding;

    protected long pageSize;

    protected String pageSizeBinding;

    protected Long maxPageSize;

    protected List<Long> pageSizeOptions;

    protected List<AggregateDefinition> aggregates;

    protected boolean usageTrackingEnabled;

    protected List<QuickFilter> quickFilters;

    protected PageProviderDefinitionBuilder(PageProviderDefinition pageProviderDefinition) {
        this.name = pageProviderDefinition.getName();
        this.enabled = pageProviderDefinition.isEnabled();
        this.properties = Objects.requireNonNullElseGet(pageProviderDefinition.getProperties(), Map::of);
        this.queryParameters = pageProviderDefinition.getQueryParameters();
        this.quotePatternParameters = pageProviderDefinition.getQuotePatternParameters();
        this.escapePatternParameters = pageProviderDefinition.getEscapePatternParameters();
        this.pattern = pageProviderDefinition.getPattern();
        this.whereClause = pageProviderDefinition.getWhereClause();
        this.searchDocumentType = pageProviderDefinition.getSearchDocumentType();
        this.sortable = pageProviderDefinition.isSortable();
        this.sortInfos = Objects.requireNonNullElseGet(pageProviderDefinition.getSortInfos(), List::of);
        this.sortInfosBinding = pageProviderDefinition.getSortInfosBinding();
        this.pageSize = pageProviderDefinition.getPageSize();
        this.pageSizeBinding = pageProviderDefinition.getPageSizeBinding();
        this.maxPageSize = pageProviderDefinition.getMaxPageSize();
        this.pageSizeOptions = Objects.requireNonNullElseGet(pageProviderDefinition.getPageSizeOptions(), List::of);
        this.aggregates = Objects.requireNonNullElseGet(pageProviderDefinition.getAggregates(), List::of);
        this.usageTrackingEnabled = pageProviderDefinition.isUsageTrackingEnabled();
        this.quickFilters = Objects.requireNonNullElseGet(pageProviderDefinition.getQuickFilters(), List::of);
    }

    public PageProviderDefinitionBuilder whereClause(WhereClauseDefinition whereClause) {
        this.whereClause = whereClause;
        return this;
    }

    /**
     * Method that allows to edit the current {@code WhereClauseDefinition whereClause} by calling the given operator.
     * <p>
     * The operator will receive the current {@link #whereClause} and its returned value will be set to this builder.
     * 
     * @return this builder
     */
    public PageProviderDefinitionBuilder whereClause(UnaryOperator<WhereClauseDefinition> whereClauseOperator) {
        whereClause = whereClauseOperator.apply(whereClause);
        return this;
    }

    public PageProviderDefinitionBuilder sortInfos(List<SortInfo> sortInfos) {
        this.sortInfos = new ArrayList<>(CollectionUtils.emptyIfNull(sortInfos));
        return this;
    }

    public PageProviderDefinitionBuilder quickFilters(List<QuickFilter> quickFilters) {
        this.quickFilters = new ArrayList<>(CollectionUtils.emptyIfNull(quickFilters));
        return this;
    }

    public PageProviderDefinition build() {
        return new PageProviderDefinitionImpl(this);
    }

    protected static class PageProviderDefinitionImpl implements PageProviderDefinition {

        protected final String name;

        protected final boolean enabled;

        protected final Map<String, String> properties;

        protected final String[] queryParameters;

        protected final boolean quotePatternParameters;

        protected final boolean escapePatternParameters;

        protected final String pattern;

        protected final WhereClauseDefinition whereClause;

        protected final String searchDocumentType;

        protected final boolean sortable;

        protected final List<SortInfo> sortInfos;

        protected final String sortInfosBinding;

        protected final long pageSize;

        protected final String pageSizeBinding;

        protected final Long maxPageSize;

        protected final List<Long> pageSizeOptions;

        protected final List<AggregateDefinition> aggregates;

        protected final boolean usageTrackingEnabled;

        protected final List<QuickFilter> quickFilters;

        public PageProviderDefinitionImpl(PageProviderDefinitionBuilder builder) {
            this.name = builder.name;
            this.enabled = builder.enabled;
            this.properties = Collections.unmodifiableMap(builder.properties);
            this.queryParameters = builder.queryParameters;
            this.quotePatternParameters = builder.quotePatternParameters;
            this.escapePatternParameters = builder.escapePatternParameters;
            this.pattern = builder.pattern;
            this.whereClause = builder.whereClause;
            this.searchDocumentType = builder.searchDocumentType;
            this.sortable = builder.sortable;
            this.sortInfos = Collections.unmodifiableList(builder.sortInfos);
            this.sortInfosBinding = builder.sortInfosBinding;
            this.pageSize = builder.pageSize;
            this.pageSizeBinding = builder.pageSizeBinding;
            this.maxPageSize = builder.maxPageSize;
            this.pageSizeOptions = Collections.unmodifiableList(builder.pageSizeOptions);
            this.aggregates = Collections.unmodifiableList(builder.aggregates);
            this.usageTrackingEnabled = builder.usageTrackingEnabled;
            this.quickFilters = Collections.unmodifiableList(builder.quickFilters);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            throw new UnsupportedOperationException("This page provider definition is immutable");
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            throw new UnsupportedOperationException("This page provider definition is immutable");
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public String[] getQueryParameters() {
            return queryParameters;
        }

        @Override
        public boolean getQuotePatternParameters() {
            return quotePatternParameters;
        }

        @Override
        public boolean getEscapePatternParameters() {
            return escapePatternParameters;
        }

        @Override
        public void setPattern(String pattern) {
            throw new UnsupportedOperationException("This page provider definition is immutable");

        }

        @Override
        public String getPattern() {
            return pattern;
        }

        @Override
        public WhereClauseDefinition getWhereClause() {
            return whereClause;
        }

        @Override
        public String getSearchDocumentType() {
            return searchDocumentType;
        }

        @Override
        public boolean isSortable() {
            return sortable;
        }

        @Override
        public List<SortInfo> getSortInfos() {
            return sortInfos;
        }

        @Override
        public String getSortInfosBinding() {
            return sortInfosBinding;
        }

        @Override
        public long getPageSize() {
            return pageSize;
        }

        @Override
        public String getPageSizeBinding() {
            return pageSizeBinding;
        }

        @Override
        public Long getMaxPageSize() {
            return maxPageSize;
        }

        @Override
        public List<Long> getPageSizeOptions() {
            return pageSizeOptions;
        }

        @Override
        public PageProviderDefinition clone() {
            throw new UnsupportedOperationException(
                    "This page provider definition is not cloneable, use #builder().build() instead");
        }

        @Override
        public List<AggregateDefinition> getAggregates() {
            return aggregates;
        }

        @Override
        public boolean isUsageTrackingEnabled() {
            return usageTrackingEnabled;
        }

        @Override
        public List<QuickFilter> getQuickFilters() {
            return quickFilters;
        }
    }
}
