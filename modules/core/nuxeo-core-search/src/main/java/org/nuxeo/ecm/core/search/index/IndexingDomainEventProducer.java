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

import static org.nuxeo.ecm.core.search.index.IndexingProcessor.SYNC_COMPUTATION_NAME;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.time.Duration;
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
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamService;

import com.fasterxml.uuid.Generators;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;

/**
 * @since 2025.0
 */
public class IndexingDomainEventProducer extends DomainEventProducer {

    private static final Logger log = LogManager.getLogger(IndexingDomainEventProducer.class);

    public static final String DISABLE_AUTO_INDEXING = "disableAutoIndexing";

    public static final String SYNC_INDEXING_FLAG = "ESSyncIndexing";

    public static final String CODEC_NAME = "avro";

    protected static final String SOURCE_NAME = "IDX";

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

    protected final ThreadLocalIndexingCommandsStacker stacker = new ThreadLocalIndexingCommandsStacker();

    protected final Codec<IndexingDomainEvent> codec;

    protected final StreamManager streamManager;

    protected final Timer syncWaitTimer;

    protected boolean anySyncCommand;

    public IndexingDomainEventProducer(String name, String stream) {
        super(name, stream);
        codec = Framework.getService(CodecService.class).getCodec(CODEC_NAME, IndexingDomainEvent.class);
        streamManager = Framework.getService(StreamService.class).getStreamManager();
        syncWaitTimer = registry.timer(MetricName.build("nuxeo.search.indexing.sync"));
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
        try (Timer.Context ignored = syncWaitTimer.time()) {
            List<LogOffset> maxOffsets = maxPerPartition(offsets);
            if (maxOffsets.size() > 1) {
                log.error("Unexpected, partition key is transaction we should have only one offset: {}", maxOffsets);
            }
            LogOffset offset = maxOffsets.getFirst();
            log.info("Waiting for sync consumer to process {} records and reach offsets: {}", offsets.size(),
                    maxOffsets);
            try {
                if (streamManager.waitFor(stream, Name.ofUrn(SYNC_COMPUTATION_NAME), offset, Duration.ofSeconds(10))) {
                    log.debug("completed");
                } else {
                    log.warn("Time out on waiting for indexer sync consumer, continuing");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

}
