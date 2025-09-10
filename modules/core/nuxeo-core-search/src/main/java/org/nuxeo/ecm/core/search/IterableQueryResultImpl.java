/*
 * (C) Copyright 2016-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.search;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Iterable query result based on scroll search results.
 *
 * @since 8.4
 */
public class IterableQueryResultImpl implements IterableQueryResult, Iterator<Map<String, Serializable>> {

    protected final SearchService searchService;

    protected final long size;

    protected SearchResponse searchResponse;

    protected boolean closed;

    protected long pos;

    protected int relativePos;

    public IterableQueryResultImpl(SearchResponse searchResponse) {
        if (searchResponse.getScrollContext() == null) {
            throw new IllegalArgumentException("The SearchQuery must be of type scroll to create an iterator");
        }
        this.searchService = Framework.getService(SearchService.class);
        this.searchResponse = searchResponse;
        if (searchResponse.isTotalAccurate()) {
            this.size = searchResponse.getTotal();
        } else {
            this.size = -1;
        }
    }

    @Override
    public void close() {
        if (!closed) {
            searchService.clearSearchScroll(searchResponse.getScrollContext());
            closed = true;
            pos = -1;
        }
    }

    @Override
    public boolean mustBeClosed() {
        return true;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public long pos() {
        return pos;
    }

    @Override
    public void skipTo(long pos) {
        checkNotClosed();
        if (pos < this.pos) {
            throw new IllegalArgumentException("Cannot go back in Iterable.");
        } else if (size >= 0 && pos > size) {
            pos = size;
        } else {
            while (pos > this.pos) {
                nextHit();
            }
        }
        this.pos = pos;
    }

    @Override
    public Iterator<Map<String, Serializable>> iterator() {
        return this; // NOSONAR this iterable does not support multiple traversals
    }

    @Override
    public boolean hasNext() {
        checkNotClosed();
        if (size < 0) {
            if (relativePos < searchResponse.getHitsCount()) {
                return true;
            } else {
                try {
                    nextHit();
                    // decrement pos and relativePos as we didn't consume the hit
                    pos--;
                    relativePos--;
                    return true;
                } catch (NoSuchElementException e) {
                    return false;
                }
            }
        }
        return pos < size;
    }

    @Override
    public Map<String, Serializable> next() {
        checkNotClosed();
        if (size >= 0 && pos == size) {
            throw new NoSuchElementException();
        }
        return nextHit();
    }

    private Map<String, Serializable> nextHit() {
        if (relativePos == searchResponse.getHitsCount()) {
            // Retrieve next scroll
            searchResponse = searchService.searchScroll(searchResponse.getScrollContext());
            relativePos = 0;
        }
        if (searchResponse.getHitsCount() == 0) {
            throw new NoSuchElementException();
        }
        pos++;
        return searchResponse.getHits().get(relativePos++).getFields();
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Query results iterator closed.");
        }
    }

}
