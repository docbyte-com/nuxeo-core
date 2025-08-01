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
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 2025.0
 */
@Inherited
@Repeatable(ConditionalIgnores.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalIgnore {

    /**
     * Condition to evaluate.
     */
    Class<? extends ConditionalIgnore.Condition> condition();

    /**
     * Optional reason why the test is ignored, reported additionally to the condition class simple name.
     */
    String cause() default "";

    /**
     * @since 2025.0
     */
    @SuppressWarnings("removal")
    interface Condition extends ConditionalIgnoreRule.Condition {

        /**
         * @return whether the test(s) should be ignored
         */
        boolean shouldIgnore();

        /**
         * Returns whether the condition needs to access Nuxeo Runtime, such as getting a service with
         * {@link org.nuxeo.runtime.api.Framework#getService(Class)}.
         * <p>
         * The condition can be evaluated at the very beginning of test run if the condition doesn't need to access
         * Nuxeo Runtime. Which will save some initialization time.
         * <p>
         * By default, evaluate condition after Nuxeo Runtime is initialized
         *
         * @return whether the condition needs to access Nuxeo Runtime
         */
        default boolean needsRuntime() {
            return true;
        }

        @Override
        default boolean supportsClassRule() {
            return !needsRuntime();
        }
    }
}
