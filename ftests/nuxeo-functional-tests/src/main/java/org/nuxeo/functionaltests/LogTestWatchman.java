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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Watchman to log info about the test and create snapshot on failure.
 *
 * @since 5.8
 */
public class LogTestWatchman extends TestWatchman {

    private static final Logger log = LogManager.getLogger(LogTestWatchman.class);

    protected final RestTestRule restHelper = new RestTestRule();

    @Override
    @SuppressWarnings("unchecked")
    public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                starting(method);
                try {
                    base.evaluate();
                    succeeded(method);
                } catch (Throwable t) {
                    failed(t, method);
                    throw t;
                } finally {
                    finished(method);
                }
            }
        };
    }

    @Override
    public void failed(Throwable e, FrameworkMethod method) {
        String className = getTestClassName(method);
        String methodName = method.getName();
        log.error("Test '{}#{}' failed", className, methodName, e);
        super.failed(e, method);
    }

    @Override
    public void finished(FrameworkMethod method) {
        log.info("Finished test '{}#{}'", () -> getTestClassName(method), method::getName);
        restHelper.finished();
        super.finished(method);
    }

    protected String getTestClassName(FrameworkMethod method) {
        return method.getMethod().getDeclaringClass().getName();
    }

    protected void logOnServer(String message) {
        restHelper.logOnServer(message);
    }

    @Override
    public void starting(FrameworkMethod method) {
        restHelper.starting();
        String message = String.format("Starting test '%s#%s'", getTestClassName(method), method.getName());
        log.info(message);
        logOnServer(message);
    }

}
