/*
 * (C) Copyright 2014-2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 *     Julien Carsique
 */
package org.nuxeo.runtime.test.runner;

import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.test.runner.FeaturesRunner.BeforeClassStatement;

public class ConditionalIgnoreRule implements TestRule, MethodRule {
    @Inject
    private RunNotifier runNotifier;

    @Inject
    private FeaturesRunner runner;

    public static class Feature implements RunnerFeature {
        protected static final ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

        @Rule
        public MethodRule methodRule() {
            return rule;
        }

        @ClassRule
        public static TestRule testRule() {
            return rule;
        }

    }

    /**
     * @deprecated since 2025.0, use {@link ConditionalIgnore} instead
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Deprecated(since = "2025.0", forRemoval = true)
    public @interface Ignore {
        Class<? extends Condition> condition();

        /**
         * Optional reason why the test is ignored, reported additionally to the condition class simple name.
         */
        String cause() default "";
    }

    /**
     * @deprecated since 2025.0, use {@link ConditionalIgnore.Condition} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public interface Condition {
        boolean shouldIgnore();

        /**
         * Returns whether this condition supports check at class level. Note: A condition supporting the class rule
         * behavior will be called before the {@link BeforeClassStatement}, at this moment Nuxeo Runtime is not fully
         * initialized and injection is not performed yet.
         * <p>
         * By default, conditions don't support it in order to keep backward compatibility.
         *
         * @since 11.1
         */
        default boolean supportsClassRule() {
            return false;
        }
    }

    /**
     * @deprecated since 11.1, {@code IsolatedClassloader} doesn't exist anymore
     */
    @Deprecated(since = "11.1")
    public static final class IgnoreIsolated implements Condition {
        boolean isIsolated = "org.nuxeo.runtime.testsuite.IsolatedClassloader".equals(
                getClass().getClassLoader().getClass().getName());

        @Override
        public boolean shouldIgnore() {
            return isIsolated;
        }
    }

    /**
     * @deprecated since 2025.0, not used
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final class IgnoreLongRunning implements Condition {
        @Override
        public boolean shouldIgnore() {
            return true;
        }
    }

    /**
     * @deprecated since 2025.0, use {@link IgnoreIfWindows} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final class IgnoreWindows extends IgnoreIfWindows {
    }

    @Override
    public Statement apply(Statement base, Description description) {
        var conditionalIgnores = runner.getAnnotations(ConditionalIgnore.class);
        Ignore ignore = runner.getConfig(Ignore.class);
        if (conditionalIgnores.isEmpty() && ignore.condition() == null) {
            return base;
        }
        return new ConditionalIgnoreStatement(base, description, conditionalIgnores, ignore,
                this::returnShouldIgnoreForClassOrNull);
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod frameworkMethod, Object target) {
        var conditionalIgnores = runner.getMethodAnnotationsWithClassFallback(ConditionalIgnore.class, frameworkMethod);
        Ignore ignore = runner.getConfig(frameworkMethod, Ignore.class);
        if (conditionalIgnores.isEmpty() && ignore.condition() == null) {
            return base;
        }
        Method method = frameworkMethod.getMethod();
        Description description = Description.createTestDescription(target.getClass(), method.getName(),
                method.getAnnotations());
        return new ConditionalIgnoreStatement(base, description, conditionalIgnores, ignore,
                (condition, cause) -> returnShouldIgnoreForMethodOrNull(condition, cause, target, method));
    }

    protected class ConditionalIgnoreStatement extends Statement {

        protected final Statement base;

        protected final Description description;

        protected final List<ConditionalIgnore> conditionalIgnores;

        @Deprecated(since = "2025.0", forRemoval = true)
        protected final ConditionalIgnoreRule.Ignore ignore;

        protected final BiFunction<Class<? extends Condition>, String, String> returnShouldIgnoreOrNull;

        protected ConditionalIgnoreStatement(Statement base, Description description,
                List<ConditionalIgnore> conditionalIgnores, Ignore ignore,
                BiFunction<Class<? extends Condition>, String, String> returnShouldIgnoreOrNull) {
            this.base = base;
            this.description = description;
            this.conditionalIgnores = conditionalIgnores;
            this.ignore = ignore;
            this.returnShouldIgnoreOrNull = returnShouldIgnoreOrNull;
        }

        @Override
        public void evaluate() throws Throwable {
            var assumptions = new ArrayList<String>();
            // handle @ConditionalIgnore
            for (ConditionalIgnore conditionalIgnore : conditionalIgnores) {
                addIgnoreNull(assumptions,
                        returnShouldIgnoreOrNull.apply(conditionalIgnore.condition(), conditionalIgnore.cause()));
            }
            // handle deprecated @ConditionalIgnoreRule.Ignore annotation
            addIgnoreNull(assumptions, returnShouldIgnoreOrNull.apply(ignore.condition(), ignore.cause()));
            if (assumptions.isEmpty()) {
                base.evaluate();
            } else {
                runNotifier.fireTestAssumptionFailed(new Failure(description,
                        new AssumptionViolatedException(String.join(System.lineSeparator(), assumptions))));
            }
        }
    }

    protected String returnShouldIgnoreForClassOrNull(Class<? extends Condition> conditionType, String cause) {
        if (conditionType != null) { // this null check is only needed for deprecated @ConditionalIgnoreRule.Ignore
            // as this is a @ClassRule / TestRule, built statement is evaluated just before @BeforeClass annotations
            // and thus before Nuxeo Runtime initialization. Condition should explicitly support the ClassRule
            // behavior to ignore tests there. If it doesn't, check will be done before test (former behavior)
            Condition condition = instantiateCondition(conditionType);
            if (condition.supportsClassRule() && condition.shouldIgnore()) {
                var assumption = conditionType.getSimpleName();
                if (isNotBlank(cause)) {
                    assumption += ": " + cause;
                }
                return assumption;
            }
        }
        return null;
    }

    protected String returnShouldIgnoreForMethodOrNull(Class<? extends Condition> conditionType, String cause,
            Object target, Method method) {
        if (conditionType != null) { // this null check is only needed for deprecated @ConditionalIgnoreRule.Ignore
            Condition condition = instantiateCondition(conditionType);
            injectCondition(condition, target, method);
            if (condition.shouldIgnore()) {
                var assumption = conditionType.getSimpleName();
                if (isNotBlank(cause)) {
                    assumption += ": " + cause;
                }
                return assumption;
            }
        }
        return null;
    }

    protected Condition instantiateCondition(Class<? extends Condition> conditionType) {
        try {
            return conditionType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException cause) {
            throw new RuntimeServiceException("Cannot instantiate condition of type " + conditionType, cause);
        }
    }

    protected void injectCondition(Condition condition, Object target, Method method) {
        var errors = new RuntimeServiceException("Cannot inject condition parameters in " + condition.getClass());
        for (Field eachField : condition.getClass().getDeclaredFields()) {
            if (!eachField.isAnnotationPresent(Inject.class)) {
                continue;
            }
            Object eachValue = null;
            if (eachField.isAnnotationPresent(Named.class)) {
                String name = eachField.getAnnotation(Named.class).value();
                if ("type".equals(name)) {
                    eachValue = target.getClass();
                } else if ("target".equals(name)) {
                    eachValue = target;
                } else if ("method".equals(name)) {
                    eachValue = method;
                }
            } else {
                Class<?> eachType = eachField.getType();
                if (eachType.equals(Class.class)) {
                    eachValue = target.getClass();
                } else if (eachType.equals(Object.class)) {
                    eachValue = target;
                } else if (eachType.equals(Method.class)) {
                    eachValue = method;
                }
            }
            if (eachValue == null) {
                continue;
            }
            eachField.setAccessible(true);
            try {
                eachField.set(condition, eachValue);
            } catch (IllegalArgumentException | IllegalAccessException cause) {
                errors.addSuppressed(new RuntimeServiceException("Cannot inject " + eachField.getName(), cause));
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
        runner.getInjector().injectMembers(condition);
    }

}
