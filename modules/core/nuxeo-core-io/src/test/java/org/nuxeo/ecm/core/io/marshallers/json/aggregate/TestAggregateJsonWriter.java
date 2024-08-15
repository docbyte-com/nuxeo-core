/*
 * (C) Copyright 2019-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *      Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json.aggregate;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_COMPLEX_STRING_PROP;
import static org.nuxeo.ecm.core.schema.test.CommonDocumentConstants.COMMON_STRING_PROP;
import static org.nuxeo.ecm.platform.query.api.AggregateConstants.AGG_TYPE_TERMS;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.local.DummyLoginFeature;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.schema.CoreSchemaFeature;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.ecm.platform.query.core.AggregateTerm;
import org.nuxeo.ecm.platform.query.core.BucketTerm;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(FeaturesRunner.class)
@Features({ CoreIOFeature.class, CoreSchemaFeature.class, DummyLoginFeature.class, LogCaptureFeature.class })
public class TestAggregateJsonWriter {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    @WithUser("user")
    public void testFetchedWithNonAdministrator() throws IOException {
        var descriptor = new AggregateDescriptor();
        descriptor.setDocumentField(COMMON_STRING_PROP);
        descriptor.setType(AGG_TYPE_TERMS);
        var aggregate = new AggregateTerm(descriptor, null);
        aggregate.setBuckets(List.of(new BucketTerm("a user", 10)));

        String json = MarshallerHelper.objectToJson(aggregate,
                CtxBuilder.fetch(AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY).get());
        assertTrue(json.contains("{\"key\":\"a user\",\"fetchedKey\":\"a user\",\"docCount\":10}"));
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = AggregateJsonWriter.class, logLevel = "WARN")
    public void testFetchedComplexField() throws IOException, JSONException {
        var descriptor = new AggregateDescriptor();
        descriptor.setDocumentField(COMMON_COMPLEX_STRING_PROP);
        descriptor.setType(AGG_TYPE_TERMS);
        var aggregate = new AggregateTerm(descriptor, null);
        aggregate.setBuckets(List.of(new BucketTerm("something", 10)));

        String json = MarshallerHelper.objectToJson(aggregate,
                CtxBuilder.fetch(AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY).get());
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertTrue(caughtEvents.isEmpty());
        String expected = """
                {
                   "entity-type": "aggregate",
                   "field": "tcc:complex/string",
                   "buckets": [
                      {
                         "key": "something",
                         "fetchedKey": "something",
                         "docCount": 10
                      }
                   ],
                   "extendedBuckets": [
                      {
                         "key": "something",
                         "fetchedKey": "something",
                         "docCount": 10
                      }
                   ]
                }""";
        JSONAssert.assertEquals(expected, json, JSONCompareMode.LENIENT);
    }

}
