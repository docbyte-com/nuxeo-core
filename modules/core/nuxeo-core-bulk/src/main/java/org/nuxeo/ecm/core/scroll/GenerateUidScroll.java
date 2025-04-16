/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      bdelbosc
 */

package org.nuxeo.ecm.core.scroll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

/**
 * A scroll implementation that generates random UUIDs. The query syntax is the number of UUID to generate.
 *
 * @since 2025.1
 */
public class GenerateUidScroll implements Scroll {

    protected int size;

    protected long total;

    protected long count;

    @Override
    public void init(ScrollRequest scrollRequest, Map<String, String> options) {
        if (!(scrollRequest instanceof GenericScrollRequest request)) {
            throw new IllegalArgumentException(
                    "Requires a GenericScrollRequest got a " + scrollRequest.getClass().getCanonicalName());
        }
        try {
            total = Long.parseLong(request.getQuery());
        } catch (NullPointerException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid query: " + request.getQuery() + " expecting a valid integer");
        }
        size = request.getSize();
    }

    @Override
    public void close() {
        // nothing
    }

    @Override
    public boolean hasNext() {
        return (count < total);
    }

    @Override
    public List<String> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<String> ret = new ArrayList<>(size);
        while (hasNext() && ret.size() < size) {
            ret.add(UUID.randomUUID().toString());
            count++;
        }
        return ret;
    }
}
