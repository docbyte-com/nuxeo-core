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
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;

/**
 * @since 2025.0
 */
public class WhereClauseDefinitionBuilder {

    protected boolean quoteFixedPartParameters;

    protected boolean escapeFixedPartParameters;

    protected List<PredicateDefinition> predicates;

    protected String fixedPart;

    protected Class<? extends Escaper> escaperClass;

    protected String selectStatement;

    protected WhereClauseDefinitionBuilder(WhereClauseDefinition whereClauseDefinition) {
        this.quoteFixedPartParameters = whereClauseDefinition.getQuoteFixedPartParameters();
        this.escapeFixedPartParameters = whereClauseDefinition.getEscapeFixedPartParameters();
        this.predicates = new ArrayList<>(
                List.of(ArrayUtils.nullToEmpty(whereClauseDefinition.getPredicates(), PredicateDefinition[].class)));
        this.fixedPart = whereClauseDefinition.getFixedPart();
        this.escaperClass = whereClauseDefinition.getEscaperClass();
        this.selectStatement = whereClauseDefinition.getSelectStatement();
    }

    public WhereClauseDefinitionBuilder fixedPart(String fixedPart) {
        this.fixedPart = fixedPart;
        return this;
    }

    public WhereClauseDefinitionBuilder selectStatement(String selectStatement) {
        this.selectStatement = selectStatement;
        return this;
    }

    public WhereClauseDefinition build() {
        return new WhereClauseDefinitionImpl(this);
    }

    protected static class WhereClauseDefinitionImpl implements WhereClauseDefinition {

        protected final boolean quoteFixedPartParameters;

        protected final boolean escapeFixedPartParameters;

        protected final PredicateDefinition[] predicates;

        protected final String fixedPart;

        protected final Class<? extends Escaper> escaperClass;

        protected final String selectStatement;

        public WhereClauseDefinitionImpl(WhereClauseDefinitionBuilder builder) {
            this.quoteFixedPartParameters = builder.quoteFixedPartParameters;
            this.escapeFixedPartParameters = builder.escapeFixedPartParameters;
            this.predicates = builder.predicates.toArray(PredicateDefinition[]::new);
            this.fixedPart = builder.fixedPart;
            this.escaperClass = builder.escaperClass;
            this.selectStatement = builder.selectStatement;
        }

        @Override
        public void setFixedPath(String fixedPart) {
            throw new UnsupportedOperationException("This where clause definition is immutable");
        }

        @Override
        public boolean getQuoteFixedPartParameters() {
            return quoteFixedPartParameters;
        }

        @Override
        public boolean getEscapeFixedPartParameters() {
            return escapeFixedPartParameters;
        }

        @Override
        public PredicateDefinition[] getPredicates() {
            return predicates;
        }

        @Override
        public void setPredicates(PredicateDefinition[] predicates) {
            throw new UnsupportedOperationException("This where clause definition is immutable");
        }

        @Override
        public String getFixedPart() {
            return fixedPart;
        }

        @Override
        public void setFixedPart(String fixedPart) {
            throw new UnsupportedOperationException("This where clause definition is immutable");
        }

        @Override
        public Class<? extends Escaper> getEscaperClass() {
            return escaperClass;
        }

        @Override
        public String getSelectStatement() {
            return selectStatement;
        }
    }
}
