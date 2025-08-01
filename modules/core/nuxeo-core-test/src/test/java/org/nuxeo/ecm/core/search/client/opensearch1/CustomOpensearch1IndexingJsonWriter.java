/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica@nuxeo.com
 */
package org.nuxeo.ecm.core.search.client.opensearch1;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.index.DefaultIndexingJsonWriter;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Custom writer to index the content of a note as a raw json
 */
public class CustomOpensearch1IndexingJsonWriter extends DefaultIndexingJsonWriter {

    @Override
    protected void writeSchemas(JsonGenerator jg, DocumentModel doc) throws IOException {
        for (String schema : doc.getSchemas()) {
            if ("note".equals(schema)) {
                // just index the clob as raw
                jg.writeFieldName("dynamic");
                jg.writeRawValue((String) doc.getPropertyValue("note:note"));
            } else {
                writeProperties(jg, doc, schema);
            }
        }
    }

}
