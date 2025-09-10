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
package org.nuxeo.ecm.webengine.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.webengine.rest.servlet.config.ServletRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Activator implements BundleActivator {

    private static final Logger log = LogManager.getLogger(Activator.class);

    private static Activator instance;

    public static Activator getInstance() {
        return instance;
    }

    protected BundleContext context;

    protected ServiceReference pkgAdm;

    @Override
    public void start(BundleContext context) {
        instance = this;
        this.context = context;
        pkgAdm = context.getServiceReference(PackageAdmin.class.getName());

        ApplicationManager.getInstance().start(context);
    }

    @Override
    public void stop(BundleContext context) {
        ApplicationManager.getInstance().stop(context);

        ServletRegistry.dispose();
        instance = null;
        context.ungetService(pkgAdm);
        pkgAdm = null;
        this.context = null;
    }

    public BundleContext getContext() {
        return context;
    }

    public PackageAdmin getPackageAdmin() {
        return (PackageAdmin) context.getService(pkgAdm);
    }
}
