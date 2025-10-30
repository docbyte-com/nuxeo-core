/*
 * (C) Copyright 2020-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.introspection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 11.5
 */
public class TestIntrospection {

    @Test
    public void testPumlConversion() {
        String json = getClassLoaderResourceAsString("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String puml = convert.getPuml();
        assertTrue(puml.contains("@startuml"));
        assertTrue(puml.contains("@enduml"));
    }

    @Test
    public void testPumlConversionSimple() {
        String json = getClassLoaderResourceAsString("data/simple.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String puml = convert.getPuml();
        assertEquals(getClassLoaderResourceAsString("data/simple.puml"), puml);
    }

    @Test
    public void testScaleUp() {
        String in = getClassLoaderResourceAsString("data/introspection-cluster.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1678439100);
        assertJsonEquals("data/scale-up.json", out);
    }

    @Test
    public void testScaleWithoutMetrics() {
        String in = getClassLoaderResourceAsString("data/introspection-cluster.json");
        // rename the metrics array so no metrics will be found
        in = in.replace("\"metrics\": [", "\"no-metrics\": [");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1678439100);
        assertJsonEquals("data/scale-no-data.json", out);
    }

    @Test
    public void testScaleIdle() {
        String in = getClassLoaderResourceAsString("data/introspection-cluster-idle.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1678439100);
        assertJsonEquals("data/scale-idle.json", out);
        // activity for a given timestamp in the future discards old metrics
        out = convert.getActivity(1778439100);
        assertJsonEquals("data/scale-no-data.json", out);
    }

    @Test
    public void testScaleConstantLoad() {
        String in = getClassLoaderResourceAsString("data/introspection-cluster-constant.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(in);
        String out = convert.getActivity(1709562437);
        assertJsonEquals("data/scale-constant.json", out);
    }

    @Test
    public void testScaleConstantLoad2() {
        String json = getClassLoaderResourceAsString("data/introspection-cluster-constant2.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String out = convert.getActivity(1756647244);
        assertJsonEquals("data/scale-constant2.json", out);
    }

    @Test
    public void testStreams() {
        String json = getClassLoaderResourceAsString("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String streams = convert.getStreams();
        assertTrue(streams, streams.contains("bulk/command"));
    }

    @Test
    public void testEmptyJson() {
        assertThrows(IllegalArgumentException.class, () -> new StreamIntrospectionConverter(null));
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter("{}");
        assertEquals("[]", convert.getStreams());
        assertEquals("[]", convert.getConsumers("bulk/whatever"));
        String activity = convert.getActivity();
        assertTrue(activity, activity.contains("scale"));
        String puml = convert.getPuml();
        assertTrue(puml, puml.contains("@startuml"));
        String d2 = convert.getD2();
        assertTrue(d2, d2.contains("# Stream Introspection"));
    }

    @Test
    public void testConsumers() {
        String json = getClassLoaderResourceAsString("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String consumers = convert.getConsumers("bulk/command");
        assertTrue(consumers.contains("bulk/scroller"));

        consumers = convert.getConsumers("unknown");
        assertEquals("[]", consumers);
    }

    @Test
    public void testD2Conversion() {
        String json = getClassLoaderResourceAsString("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String d2 = convert.getD2();
        assertTrue(d2.contains("# Stream Introspection"));
        assertTrue(d2.contains("stream_bulk_command"));
        assertTrue(d2, d2.contains("group_bulk_scroller"));
    }

    @Test
    public void testD2ConversionSimple() {
        String json = getClassLoaderResourceAsString("data/simple.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String d2 = convert.getD2();
        assertEquals(getClassLoaderResourceAsString("data/simple.d2"), d2);
    }

    @Test
    public void generateExpectedD2Output() {
        String json = getClassLoaderResourceAsString("data/simple.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String d2 = convert.getD2(null, true);
        // System.out.println("Generated D2 content:");
        // System.out.println(d2);
    }

    @Test
    public void testD2ConversionWithFiltering() {
        String json = getClassLoaderResourceAsString("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);

        // Test without filtering
        String d2NoFilter = convert.getD2();
        assertTrue(d2NoFilter.contains("stream_bulk_command"));
        assertTrue(d2NoFilter.contains("group_bulk_scroller"));

        // Test with pattern filtering to exclude "bulk/" patterns
        var excludePatterns = List.of("bulk/");
        String d2WithPatternFilter = convert.getD2(excludePatterns, false);
        assertFalse(d2WithPatternFilter.contains("stream_bulk_command"));
        assertFalse(d2WithPatternFilter.contains("group_bulk_scroller"));

        // Test with inactive filtering (should exclude both inactive computations and empty streams)
        String d2WithInactiveFilter = convert.getD2(Collections.emptyList(), true);
        assertTrue(d2WithInactiveFilter.contains("# Stream Introspection"));
        // Should exclude streams with 0 records (stream-empty class)
        assertFalse(d2WithInactiveFilter.contains("stream_work_collections")); // This stream has 0 records

        // Test with both filters
        String d2WithBothFilters = convert.getD2(excludePatterns, true);
        assertFalse(d2WithBothFilters.contains("bulk_"));
    }

    protected static void assertJsonEquals(String resourcePath, String actual) {
        ObjectMapper mapper = new ObjectMapper();
        String expected = getClassLoaderResourceAsString(resourcePath);
        try {
            String prettyExpected = mapper.readTree(expected).toPrettyString();
            String prettyActual = mapper.readTree(actual).toPrettyString();
            assertEquals(prettyExpected, prettyActual);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to prettify JSON", e);
        }
    }
}
