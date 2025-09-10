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

package org.nuxeo.ecm.core.storage.action;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Bulk Action to fix the binary fulltext storage when switching storage from repository to blob using
 * nuxeo.vcs.fulltext.storedInBlob=true on an existing instance.
 * <p>
 * This action is copying existing fulltext from the repository to blobs without extracting the binary fulltext or
 * triggering any events or reindexing.
 *
 * @since 2023.27
 */
public class FixBinaryFulltextStorageAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "fixBinaryFulltextStorage";

    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(FixBinaryFulltextStorageComputation::new,
                               Arrays.asList(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class FixBinaryFulltextStorageComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(FixBinaryFulltextStorageComputation.class);

        public FixBinaryFulltextStorageComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            @SuppressWarnings("rawtypes")
            Session lowSession = ((AbstractSession) session).getSession();
            int updated = 0;
            for (String id : ids) {
                Document doc = lowSession.getDocumentByUUID(id);
                if (doc.isProxy()) {
                    delta.incrementSkipCount();
                    continue;
                }
                String fulltext = (String) doc.getPropertyValue("ecm:fulltextBinary");
                if (AbstractSession.isFulltextValueABlobKey(fulltext)) {
                    delta.incrementSkipCount();
                    continue;
                }
                updated++;
                doc.setSystemProp("fulltextBinary", fulltext);
                String key = (String) doc.getPropertyValue("ecm:fulltextBinary");
                log.warn("Move fulltext of: {}, to blob: {}, length: {}", id, key, fulltext.length());
            }
            if (updated > 0) {
                try {
                    lowSession.save();
                } catch (PropertyException e) {
                    // corrupted docs
                    log.warn("Cannot save session", e);
                }
            }
        }
    }
}
