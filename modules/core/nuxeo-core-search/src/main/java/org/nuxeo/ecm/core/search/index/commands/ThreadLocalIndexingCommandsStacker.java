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
package org.nuxeo.ecm.core.search.index.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 2025.0
 */
public class ThreadLocalIndexingCommandsStacker extends IndexingCommandsStacker {

    protected static final ThreadLocal<Map<String, IndexingCommands>> transactionCommands = ThreadLocal.withInitial(
            HashMap::new);

    public static final ThreadLocal<Boolean> useSyncIndexing = new ThreadLocal<>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }

        @Override
        public void set(Boolean value) {
            super.set(value);
            if (Boolean.TRUE.equals(value)) {
                // switch existing stack to sync
                for (IndexingCommands cmds : transactionCommands.get().values()) {
                    for (IndexingCommand cmd : cmds.getCommands()) {
                        cmd.makeSync();
                    }
                }
            }
        }
    };

    @Override
    public Map<String, IndexingCommands> getAllCommands() {
        return transactionCommands.get();
    }

    @Override
    protected boolean isSyncIndexingByDefault() {
        Boolean ret = useSyncIndexing.get();
        if (ret == null) {
            ret = false;
        }
        return ret;
    }
}
