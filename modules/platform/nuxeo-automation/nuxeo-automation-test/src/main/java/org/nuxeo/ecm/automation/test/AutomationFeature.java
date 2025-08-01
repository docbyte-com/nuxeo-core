/*
 * (C) Copyright 2013-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test;

import jakarta.inject.Inject;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.automation.scripting.AutomationScriptingFeature;
import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.AutomationCoreFeature;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.automation.features.AutomationFeaturesFeature;
import org.nuxeo.ecm.automation.io.AutomationIOFeature;
import org.nuxeo.ecm.automation.server.AutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.google.inject.Binder;

/**
 * The feature deploys the {@link PlatformFeature} and all automation bundles.
 *
 * @since 5.7
 * @since 5.6-HF02
 * @since 2025.0, the features deploys all automation bundles
 */
@Features({ //
        AutomationCoreFeature.class, //
        AutomationIOFeature.class, //
        AutomationFeaturesFeature.class, //
        AutomationServerFeature.class, //
        AutomationScriptingFeature.class, //
        PlatformFeature.class })
public class AutomationFeature implements RunnerFeature {

    @Inject
    protected TracerFactory tracerFactory;

    @Inject
    protected CoreFeature coreFeature;

    protected OperationContext context;

    protected OperationCallback tracer;

    protected OperationContext getContext() {
        if (context == null) {
            CoreSession session = coreFeature.getCoreSession();
            context = new OperationContext(session);
            if (tracer != null) {
                context.setCallback(tracer);
            }
        }
        return context;
    }

    protected OperationCallback getTracer() {
        if (tracer == null) {
            tracer = tracerFactory.newTracer();
            if (context != null) {
                context.setCallback(tracer);
            }
        }
        return tracer;
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(OperationContext.class).toProvider(this::getContext).in(AutomationScope.INSTANCE);
        binder.bind(OperationCallback.class).toProvider(this::getTracer).in(AutomationScope.INSTANCE);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        AutomationScope.INSTANCE.enter();
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        AutomationScope.INSTANCE.exit();
        if (context != null) {
            context.close();
        }
        context = null;
        tracer = null;
        tracerFactory.clearTraces();
    }
}
