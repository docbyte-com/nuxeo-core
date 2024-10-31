/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.model.io.BlobReader;
import org.nuxeo.ecm.webengine.model.io.BlobWriter;
import org.nuxeo.ecm.webengine.model.io.DocumentBlobHolderWriter;
import org.nuxeo.ecm.webengine.model.io.DownloadContextBlobHolderWriter;
import org.nuxeo.ecm.webengine.model.io.FileWriter;
import org.nuxeo.ecm.webengine.model.io.MultivaluedMapProvider;
import org.nuxeo.ecm.webengine.model.io.ScriptFileWriter;
import org.nuxeo.ecm.webengine.model.io.TemplateViewWriter;
import org.nuxeo.ecm.webengine.model.io.TemplateWriter;
import org.nuxeo.ecm.webengine.model.io.URLWriter;

/**
 * A web application configured by a module.xml file.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        var result = new HashSet<Class<?>>();
        result.add(BlobReader.class);
        result.add(WebEngineExceptionMapper.class);
        result.add(TemplateWriter.class);
        result.add(ScriptFileWriter.class);
        result.add(DocumentBlobHolderWriter.class);
        result.add(DownloadContextBlobHolderWriter.class);
        result.add(BlobWriter.class);
        result.add(FileWriter.class);
        result.add(URLWriter.class);
        result.add(TemplateViewWriter.class);
        result.add(JsonNuxeoExceptionWriter.class);
        result.add(MultivaluedMapProvider.class);
        return result;
    }
}
