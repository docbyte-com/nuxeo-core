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
package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.collections4.list.AbstractSerializableListDecorator;

/**
 * @since 2025.0
 * @deprecated since 2025.0, use {@link org.nuxeo.audit.api.LogEntryList} instead
 */
@Deprecated(since = "2025.0", forRemoval = true)
public class LogEntryList2 extends AbstractSerializableListDecorator<LogEntry> implements List<LogEntry>, Serializable {

    protected final long totalSize;

    /**
     * Constructor that wraps (not copies).
     *
     * @param list the list to decorate, must not be null
     * @throws NullPointerException if list is null
     */
    public LogEntryList2(List<LogEntry> list, long totalSize) {
        super(list);
        this.totalSize = totalSize;
    }

    /**
     * Returns the total size of the bigger list this is a part of.
     *
     * @return the total size
     */
    public long getTotalSize() {
        return totalSize;
    }
}
