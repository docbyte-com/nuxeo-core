/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.rest.scan;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.objectweb.asm.ClassReader;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Scanner {

    protected final Bundle bundle;

    protected final String packageBase;

    protected final Map<String, Collection<Class<? extends Annotation>>> collectors;

    public Scanner(Bundle bundle, String packageBase) {
        this(bundle, packageBase, Path.class, Provider.class);
    }

    @SafeVarargs
    public Scanner(Bundle bundle, String packageBase, Class<? extends Annotation>... annotations) {
        this.bundle = bundle;
        this.packageBase = Objects.requireNonNullElse(packageBase, "/");
        this.collectors = new HashMap<>();
        for (Class<?> annotation : annotations) {
            this.collectors.put(getDescriptorName(annotation), new HashSet<>());
        }
    }

    @SuppressWarnings("unchecked")
    public void scan() throws ReflectiveOperationException, IOException {
        Enumeration<URL> urls = bundle.findEntries(packageBase, "*.class", true);
        if (urls == null) {
            return;
        }
        Set<String> annotations = collectors.keySet();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try (InputStream in = url.openStream()) {
                ClassReader cr = new ClassReader(in);
                AnnotationReader reader = new AnnotationReader(annotations);
                cr.accept(reader, null, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                if (reader.hasResults()) {
                    String cname = reader.getClassName();
                    for (String anno : reader.getResults()) {
                        collectors.get(anno).add(bundle.loadClass(cname));
                    }
                }
            }
        }
    }

    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<>();
        for (Collection<Class<? extends Annotation>> c : collectors.values()) {
            result.addAll(c);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> Set<Class<A>> getClasses(Class<A> annotation) {
        return collectors.get(getDescriptorName(annotation))
                         .stream()
                         .map(a -> (Class<A>) a)
                         .collect(Collectors.toSet());
    }

    private String getDescriptorName(Class<?> annotation) {
        return 'L' + annotation.getName().replaceAll("\\.", "/") + ';';
    }
}
