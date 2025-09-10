/*
 * (C) Copyright 2006-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.scripting;

import static org.nuxeo.common.utils.FileUtils.checkPathTraversal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ScriptFile {

    public static final String ROOT_PATH = Framework.getService(WebEngine.class).getRootDirectory().getAbsolutePath();

    protected File file;

    protected final String ext;

    protected ScriptFile(String ext) {
        this.ext = ext;
    }

    // TODO should remove the typed file name
    public ScriptFile(File file) throws IOException {
        String name = file.getName();
        checkPathTraversal(name);
        int p = name.lastIndexOf('.');
        if (p > -1) {
            ext = name.substring(p + 1);
        } else {
            ext = "";
        }
        this.file = file.getCanonicalFile();
    }

    public boolean isTemplate() {
        return "ftl".equals(ext);
    }

    public File getFile() {
        return file;
    }

    public String getExtension() {
        return ext;
    }

    public String getAbsolutePath() {
        return getFile().getAbsolutePath();
    }

    public String getRelativePath() {
        return getAbsolutePath().substring(ROOT_PATH.length());
    }

    public String getFileName() {
        return getFile().getName();
    }

    public String getURL() throws MalformedURLException {
        return getFile().toURI().toURL().toExternalForm();
    }

    public URL toURL() throws MalformedURLException {
        return getFile().toURI().toURL();
    }

    public URI toURI() {
        return getFile().toURI();
    }

    public long lastModified() {
        return getFile().lastModified();
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(getFile());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
