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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.stream.DomainEventProducer;
import org.nuxeo.ecm.core.search.index.commands.IndexingCommand;
import org.nuxeo.ecm.core.search.index.commands.IndexingCommands;
import org.nuxeo.ecm.core.search.index.commands.ThreadLocalIndexingCommandsStacker;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

import com.fasterxml.uuid.Generators;

/**
 * @since 2025.0
 */
public class IndexingDomainEventProducer extends DomainEventProducer {

    private static final Logger log = LogManager.getLogger(IndexingDomainEventProducer.class);

    public static final String DISABLE_AUTO_INDEXING = "disableAutoIndexing";

    public static final String SYNC_INDEXING_FLAG = "ESSyncIndexing";

    public static final String CODEC_NAME = "avro";

    protected static final String SOURCE_NAME = "IDX";

    protected final ThreadLocalIndexingCommandsStacker stacker = new ThreadLocalIndexingCommandsStacker();

    protected final Codec<IndexingDomainEvent> codec;

    protected boolean anySyncCommand;

    public IndexingDomainEventProducer(String name, String stream) {
        super(name, stream);
        codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, IndexingDomainEvent.class);
    }

    @Override
    public void addEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext docCtx)) {
            // don't process Events that are not tied to Documents
            return;
        }
        stacker.stackCommand(docCtx, event.getName());
    }

    @Override
    public List<Record> getDomainEvents() {
        Map<String, IndexingCommands> commands = stacker.getAllCommands();
        List<Record> ret = new ArrayList<>(commands.size());
        String key = Generators.timeBasedEpochGenerator().generate().toString();
        anySyncCommand = false;
        for (Map.Entry<String, IndexingCommands> entry : commands.entrySet()) {
            for (IndexingCommand command : entry.getValue().getCommands()) {
                ret.add(buildRecordFromCommand(key, command));
                anySyncCommand |= command.isSync();
            }
        }
        stacker.getAllCommands().clear();
        markLastRecordAsEndOfBatch(ret);
        return ret;
    }

    protected void markLastRecordAsEndOfBatch(List<Record> ret) {
        if (ret.isEmpty()) {
            return;
        }
        Record lastRecord = ret.getLast();
        lastRecord.setFlags(EnumSet.of(Record.Flag.END_OF_BATCH));
    }

    private Record buildRecordFromCommand(String key, IndexingCommand command) {
        IndexingDomainEvent event = new IndexingDomainEvent();
        event.source = SOURCE_NAME;
        event.event = command.getType().name();
        event.sync = command.isSync();
        event.recurse = command.isRecurse();
        event.repository = command.getRepositoryName();
        event.docId = command.getTargetDocumentId();
        event.path = command.getPath();
        return Record.of(key, codec.encode(event));
    }

    @Override
    public void postCommitHook(List<LogOffset> offsets) {
        if (!anySyncCommand || offsets.isEmpty()) {
            log.debug("No sync command");
            return;
        }
        List<LogOffset> maxOffsets = maxPerPartition(offsets);
        log.warn("Waiting for sync consumer to process {} records and reach offsets: {}", offsets.size(), maxOffsets);
        // TODO: implement
    }

}
