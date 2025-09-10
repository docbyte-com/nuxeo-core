/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @since 2025.0
 */
public class BulkIndexingResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 20250116L;

    protected final SearchIndex index;

    protected final List<IndexingFailure> failures;

    protected BulkIndexingResponse(Builder builder) {
        this.index = builder.index;
        this.failures = Collections.unmodifiableList(builder.failures);
    }

    /**
     * Returns the SearchIndex used to perform the query.
     */
    public SearchIndex getSearchIndex() {
        return index;
    }

    /**
     * Returns the list of indexing failures.
     */
    public List<IndexingFailure> getFailures() {
        return failures;
    }

    /**
     * Returns {@code true} if there is one or more indexing failure.
     */
    public boolean hasFailure() {
        return !failures.isEmpty();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Builder buildResponse(SearchIndex index) {
        return new Builder(index);
    }

    public static class Builder {

        protected final SearchIndex index;

        protected final List<IndexingFailure> failures;

        protected Builder(SearchIndex index) {
            if (index == null) {
                throw new IllegalArgumentException("The search index is required");
            }
            this.index = index;
            this.failures = new ArrayList<>();
        }

        public Builder addFailure(String documentId, String failure) {
            if (isEmpty(documentId)) {
                throw new IllegalArgumentException("Missing documentId");
            }
            if (isEmpty(documentId)) {
                throw new IllegalArgumentException("Missing failure");
            }
            failures.add(new IndexingFailure(documentId, failure));
            return this;
        }

        public BulkIndexingResponse build() {
            return new BulkIndexingResponse(this);
        }
    }

    public record IndexingFailure(String documentId, String failureMessage) {
    }

}
