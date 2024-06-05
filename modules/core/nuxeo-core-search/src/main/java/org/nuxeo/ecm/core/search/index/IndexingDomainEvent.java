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
package org.nuxeo.ecm.core.search.index;

import java.io.Serial;
import java.io.Serializable;

import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Domain Event for Indexing.
 *
 * @since 2025.0
 */
public class IndexingDomainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 20240521L;

    protected String source;

    protected String event;

    protected boolean sync;

    protected boolean recurse;

    protected String repository;

    protected String docId;

    @Nullable
    protected String path;

    public String getSource() {
        return source;
    }

    public String getEvent() {
        return event;
    }

    public boolean isSync() {
        return sync;
    }

    public boolean isRecurse() {
        return recurse;
    }

    public String getRepository() {
        return repository;
    }

    public String getDocId() {
        return docId;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
