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
package org.nuxeo.ecm.core.convert.cache;

import static java.util.Comparator.comparing;

import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @since 2025.0
 */
public class ConversionCacheGCTask implements Runnable {

    private static final Logger log = LogManager.getLogger(ConversionCacheGCTask.class);

    protected final long maxSizeKB;

    public ConversionCacheGCTask(long maxSizeKB) {
        this.maxSizeKB = maxSizeKB;
    }

    @Override
    public void run() {
        log.debug("GC Thread awake, see if there is some work to be done");
        long currentSize = ConversionCacheHolder.getCacheKeys()
                                                .stream()
                                                .map(ConversionCacheHolder::getCacheEntry)
                                                .filter(Objects::nonNull)
                                                .mapToLong(ConversionCacheEntry::getDiskSpaceUsageInKB)
                                                .sum();
        long toDeleteSize = currentSize - maxSizeKB;
        if (toDeleteSize < 0) {
            log.info("No GC needed, currentSize: {} < maxSize: {}", currentSize, maxSizeKB);
            return;
        }
        if (maxSizeKB < 0) {
            log.info("Configured maxSize is negative, clean up everything"); // mainly for testing
            toDeleteSize = currentSize;
        }
        log.debug("GC needed to free: {}KB of data", toDeleteSize);
        var sortedKeys = ConversionCacheHolder.getCacheKeys()
                                              .stream()
                                              .map(key -> Pair.of(key, ConversionCacheHolder.getCacheEntry(key)))
                                              .filter(pair -> pair.getRight() != null)
                                              .sorted(comparing(pair -> pair.getRight().getLastAccessedTime()))
                                              .map(Pair::getLeft)
                                              .toList();
        long deletedSize = 0;
        for (var key : sortedKeys) {
            if (deletedSize > toDeleteSize) {
                break;
            }
            log.trace("GC the conversion with key: {}", key);
            var cacheEntry = ConversionCacheHolder.getCacheEntry(key);
            deletedSize += cacheEntry.getDiskSpaceUsageInKB();
            ConversionCacheHolder.removeFromCache(key);
        }
        log.debug("GC terminated");
    }
}
