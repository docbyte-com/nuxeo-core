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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @since 2025.0
 */
public class IndexingRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 20240617L;

    private static final Logger log = LogManager.getLogger(IndexingRequest.class);

    protected final boolean delete;

    protected final boolean recurse;

    protected final String documentId;

    protected final String documentPath;

    protected String source;

    private IndexingRequest(boolean delete, boolean recurse, String documentId, String documentPath, String source) {
        this.delete = delete;
        this.recurse = recurse;
        if (recurse && !delete) {
            throw new IllegalArgumentException("Only delete request can be recursive");
        }
        if (recurse && delete && isEmpty(documentPath)) {
            throw new IllegalArgumentException("For recursive delete a document path is required");
        }
        if (isEmpty(documentId)) {
            throw new IllegalArgumentException("No documentId provided");
        }
        this.documentId = documentId;
        this.documentPath = documentPath;
        this.source = source;
    }

    public static IndexingRequest upsert(String documentId) {
        return new IndexingRequest(false, false, documentId, null, null);
    }

    public static IndexingRequest upsertWithSource(String documentId, String source) {
        return new IndexingRequest(false, false, documentId, null, source);
    }

    public static IndexingRequest delete(String documentId) {
        return new IndexingRequest(true, false, documentId, null, null);
    }

    public static IndexingRequest deleteRecursive(String documentId, String documentPath) {
        return new IndexingRequest(true, true, documentId, documentPath, null);
    }

    public boolean isDelete() {
        return delete;
    }

    public boolean isDeleteRecursive() {
        return delete && recurse;
    }

    public boolean isUpsert() {
        return !delete;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    protected void setSource(String source) {
        this.source = source;
    }

    public boolean hasSource() {
        return this.source != null;
    }

    public String getSource() {
        // TODO: Raise or add a missing source message in the doc with a searchable marker
        return Objects.requireNonNullElseGet(source, () -> {
            log.warn("The document: {} doesn't have a source", documentId);
            return "{\"ecm:uuid\":\"" + documentId + "\"}";
        });
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
