/*
 * (C) Copyright 2015-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 */
package org.nuxeo.uidgen.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.ConcurrentException;
import org.nuxeo.runtime.opensearch1.OpenSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(OpenSearchFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/uidgenerator-service.xml")
@Deploy("org.nuxeo.uidgen.opensearch1")
@Deploy("org.nuxeo.uidgen.opensearch1:OSGI-INF/opensearch-uidgen-test-contrib.xml")
public class TestSequenceGeneratorWithOpenSearch {

    @Inject
    protected UIDGeneratorService uidGeneratorService;

    @Test
    public void testIncrement() {
        UIDSequencer seq = uidGeneratorService.getSequencer();
        assertNotNull(seq);
        assertTrue(seq.getClass().isAssignableFrom(OpenSearchUIDSequencer.class));

        assertEquals(1, seq.getNextLong("myseq"));
        assertEquals(2, seq.getNextLong("myseq"));
        assertEquals(3L, seq.getNextLong("myseq"));
        assertEquals(1, seq.getNextLong("myseq2"));
        assertEquals(4, seq.getNextLong("myseq"));
        assertEquals(2, seq.getNextLong("myseq2"));
    }

    @Test
    public void testInitSequence() {
        UIDSequencer seq = uidGeneratorService.getSequencer();
        seq.getNextLong("mySequence");
        seq.getNextLong("mySequence");

        assertTrue(seq.getNextLong("mySequence") > 1);
        // initSequence will work only for greater value
        seq.initSequence("mySequence", 1_000_000L);
        assertEquals(1_000_001L, seq.getNextLong("mySequence"));
        assertEquals(1_000_002L, seq.getNextLong("mySequence"));
        seq.initSequence("another", 3_147_483_647L);
        assertTrue("Sequence should be a long", seq.getNextLong("another") > 3_147_483_647L);
        // an existing sequence can only be re-initialized to a higher value
        assertThrows(ConcurrentException.class, () -> seq.initSequence("mySequence", 100));
        seq.initSequence("mySequence", 1_000_0003L);
    }

    @Test
    @Ignore("NXP-20582: timeout waiting termination")
    public void testConcurrency() throws Exception {
        final String seqName = "mt";
        int nbCalls = 5000;

        final UIDSequencer seq = uidGeneratorService.getSequencer();
        var tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(nbCalls + 1));

        for (int i = 0; i < nbCalls; i++) {
            tpe.submit(() -> seq.getNextLong(seqName));
        }

        tpe.shutdown();
        boolean finish = tpe.awaitTermination(20, TimeUnit.SECONDS);
        assertTrue("timeout", finish);

        assertEquals(nbCalls + 1, seq.getNextLong(seqName));
    }

    @Test
    public void testBlockOfSequences() {
        UIDSequencer seq = uidGeneratorService.getSequencer();
        String key = "blockKey";
        int size = 1000;
        seq.initSequence(key, 0L);
        List<Long> block = seq.getNextBlock(key, size);
        assertNotNull(block);
        assertEquals(size, block.size());
        assertTrue(block.get(0) < block.get(1));
        assertTrue(block.get(size - 2) < block.get(size - 1));
        assertTrue(block.get(size - 1) < seq.getNextLong(key));
    }
}
