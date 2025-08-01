/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.loader.store;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class loader allows modifying the stores (adding/removing). Mutable operations are thread safe.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ResourceStoreClassLoader extends ClassLoader implements Cloneable {

    private static final Logger log = LogManager.getLogger(ResourceStoreClassLoader.class);

    private volatile ResourceStore[] stores;

    private final LinkedHashSet<ResourceStore> cp; // class path

    public ResourceStoreClassLoader(final ClassLoader pParent) {
        this(pParent, new LinkedHashSet<>());
    }

    protected ResourceStoreClassLoader(final ClassLoader pParent, LinkedHashSet<ResourceStore> cp) {
        super(pParent);
        this.cp = cp;
        if (!cp.isEmpty()) {
            stores = cp.toArray(new ResourceStore[0]);
        }
    }

    public synchronized boolean addStore(ResourceStore store) {
        if (cp.add(store)) {
            stores = cp.toArray(new ResourceStore[0]);
            return true;
        }
        return false;
    }

    public synchronized boolean removeStore(ResourceStore store) {
        if (cp.remove(store)) {
            stores = cp.toArray(new ResourceStore[0]);
            return true;
        }
        return false;
    }

    @Override
    public synchronized ResourceStoreClassLoader clone() {
        return new ResourceStoreClassLoader(getParent(), new LinkedHashSet<>(cp));
    }

    public ResourceStore[] getStores() {
        return stores;
    }

    protected Class<?> fastFindClass(final String name) {
        ResourceStore[] _stores = stores; // use a local variable
        if (_stores != null) {
            for (final ResourceStore store : _stores) {
                final byte[] clazzBytes = store.getBytes(convertClassToResourcePath(name));
                if (clazzBytes != null) {
                    log.trace("{} found class: {} ({} bytes)", getId(), name, clazzBytes.length);
                    doDefinePackage(name);
                    return defineClass(name, clazzBytes, 0, clazzBytes.length);
                }
            }
        }
        return null;
    }

    /**
     * Without this method getPackage() returns null
     */
    protected void doDefinePackage(String name) {
        int i = name.lastIndexOf('.');
        if (i > -1) {
            String pkgname = name.substring(0, i);
            Package pkg = getDefinedPackage(pkgname);
            if (pkg == null) {
                definePackage(pkgname, null, null, null, null, null, null, null);
            }
        }
    }

    @Override
    protected URL findResource(String name) {
        return streamResources(name).findFirst().orElse(null);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        return streamResources(name).collect(
                Collectors.collectingAndThen(Collectors.toList(), Collections::enumeration));
    }

    private Stream<URL> streamResources(String name) {
        ResourceStore[] _stores = stores; // use a local variable
        if (_stores != null) {
            return Stream.of(_stores)
                         .map(store -> store.getURL(name))
                         .filter(Objects::nonNull)
                         .peek(url -> log.trace("{} found resource: {}", getId(), name));
        }
        return Stream.empty();
    }

    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);

        if (clazz == null) {
            clazz = fastFindClass(name);

            if (clazz == null) {

                final ClassLoader parent = getParent();
                if (parent != null) {
                    clazz = parent.loadClass(name);
                } else {
                    throw new ClassNotFoundException(name);
                }

            } else {
                log.debug("{} loaded from store: {}", getId(), name);
            }
        }

        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final Class<?> clazz = fastFindClass(name);
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = findResources(name);
        if (urls == null) {
            final ClassLoader parent = getParent();
            if (parent != null) {
                urls = parent.getResources(name);
            }
        }
        return urls;
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null) {
            final ClassLoader parent = getParent();
            if (parent != null) {
                url = parent.getResource(name);
            }
        }
        return url;
    }

    protected String getId() {
        return this + "[" + this.getClass().getClassLoader() + "]";
    }

    /**
     * org.my.Class -&gt; org/my/Class.class
     */
    public static String convertClassToResourcePath(final String pName) {
        return pName.replace('.', '/') + ".class";
    }

}
