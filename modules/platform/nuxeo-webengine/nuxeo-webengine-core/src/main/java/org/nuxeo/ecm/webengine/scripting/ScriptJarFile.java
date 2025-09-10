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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.webengine.scripting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.0
 */
public class ScriptJarFile extends ScriptFile {

    protected final URL url;

    public ScriptJarFile(URL url) {
        super(FilenameUtils.getExtension(url.getPath()));
        this.url = url;
    }

    @Override
    public File getFile() {
        if (file == null) {
            synchronized (this) {
                if (file == null) {
                    try {
                        var tempFile = Framework.createTempFile("template-", '.' + ext);
                        IOUtils.copy(url, tempFile);
                        file = tempFile;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return file;
    }

    @Override
    public String getAbsolutePath() {
        return url.getFile();
    }

    @Override
    public String getFileName() {
        return FilenameUtils.getName(url.getFile());
    }

    @Override
    public String getURL() throws MalformedURLException {
        return url.toExternalForm();
    }

    @Override
    public URL toURL() throws MalformedURLException {
        return url;
    }

    @Override
    public URI toURI() {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long lastModified() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }
}
