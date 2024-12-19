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
package org.nuxeo.ecm.automation.core.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OutputCollector;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
public class OperationTypeImpl extends AbstractOperationType {

    /**
     * The operation ID - used for lookups.
     */
    protected String id;

    /**
     * The operation ID Aliases array.
     *
     * @since 7.1
     */
    protected String[] aliases;

    /**
     * The operation type
     */
    protected Class<?> type;

    protected String contributingComponent;

    /**
     * Invocable methods
     */
    protected List<InvokableMethod> methods;

    /**
     * Injectable parameters. a map between the parameter name and the Field object
     */
    protected Map<String, Field> params;

    /**
     * Fields that should be injected from context
     */
    protected List<Field> injectableFields;

    /** @since 2025.0 */
    public OperationTypeImpl(String id, Class<?> type, String contributingComponent) {
        Operation anno = type.getAnnotation(Operation.class);
        if (anno == null) {
            throw new IllegalArgumentException(
                    "Invalid operation class: " + type + ". No @Operation annotation found on class.");
        }
        this.id = id;
        this.aliases = anno.aliases();
        this.type = type;
        this.contributingComponent = contributingComponent;
        this.methods = initMethods(this, type);
        this.params = initParams(type);
        this.injectableFields = initFields(type);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String[] getAliases() {
        return aliases;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public OperationDocumentation getDocumentation() {
        Operation op = type.getAnnotation(Operation.class);
        OperationDocumentation doc = new OperationDocumentation(op.id());
        doc.label = op.label();
        doc.requires = op.requires();
        doc.category = op.category();
        doc.since = op.since();
        doc.deprecatedSince = op.deprecatedSince();
        doc.addToStudio = op.addToStudio();
        doc.setAliases(op.aliases());
        doc.implementationClass = type.getName();
        if (doc.requires.isEmpty()) {
            doc.requires = null;
        }
        if (doc.label.isEmpty()) {
            doc.label = doc.id;
        }
        doc.description = op.description();
        // load parameters information
        List<OperationDocumentation.Param> paramsAccumulator = new LinkedList<>();
        for (Field field : params.values()) {
            Param p = field.getAnnotation(Param.class);
            OperationDocumentation.Param param = new OperationDocumentation.Param();
            param.name = p.name();
            param.description = p.description();
            param.type = getParamDocumentationType(field.getType());
            param.widget = p.widget();
            if (param.widget.isEmpty()) {
                param.widget = null;
            }
            param.order = p.order();
            param.values = p.values();
            param.required = p.required();
            paramsAccumulator.add(param);
        }
        Collections.sort(paramsAccumulator);
        doc.params = paramsAccumulator.toArray(OperationDocumentation.Param[]::new);
        // load signature
        doc.signature = buildSignature(methods, InvokableMethod::getOutputType).toArray(String[]::new);
        // widgets descriptor
        return doc;
    }

    @Override
    public String getContributingComponent() {
        return contributingComponent;
    }

    @Override
    public InvokableMethod[] getMethodsMatchingInput(Class<?> in) {
        return methods.stream()
                      .map(method -> new Match(method, method.inputMatch(in)))
                      .filter(match -> match.priority > 0)
                      .sorted()
                      .map(match -> match.method)
                      .toArray(InvokableMethod[]::new);
    }

    /**
     * @since 5.7.2
     */
    @Override
    public List<InvokableMethod> getMethods() {
        return methods;
    }

    @Override
    public Object newInstance(OperationContext ctx, Map<String, Object> args) throws OperationException {
        Object obj;
        try {
            obj = type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new OperationException(e);
        }
        inject(ctx, args, obj);
        return obj;
    }

    public void inject(OperationContext ctx, Map<String, ?> args, Object target) throws OperationException {
        var automationService = Framework.getService(AutomationService.class);
        for (Map.Entry<String, Field> entry : params.entrySet()) {
            Object obj = resolveObject(ctx, entry.getKey(), args);
            if (obj == null) {
                // We did not resolve object according to its param name, let's
                // check with potential alias
                String[] entryAliases = entry.getValue().getAnnotation(Param.class).alias();
                if (entryAliases != null) {
                    for (String alias : entry.getValue().getAnnotation(Param.class).alias()) {
                        obj = resolveObject(ctx, alias, args);
                        if (obj != null) {
                            break;
                        }
                    }
                }
            }
            if (obj == null) {
                if (entry.getValue().getAnnotation(Param.class).required()) {
                    throw new OperationException("Failed to inject parameter '" + entry.getKey()
                            + "'. Seems it is missing from the context. Operation: " + getId());
                } // else do nothing
            } else {
                Field field = entry.getValue();
                Class<?> cl = obj.getClass();
                if (!field.getType().isAssignableFrom(cl)) {
                    // try to adapt
                    obj = automationService.getAdaptedValue(ctx, obj, field.getType());
                }
                try {
                    field.set(target, obj);
                } catch (ReflectiveOperationException e) {
                    throw new OperationException(e);
                }
            }
        }
        for (Field field : injectableFields) {
            Object obj = ctx.getAdapter(field.getType());
            try {
                field.set(target, obj);
            } catch (ReflectiveOperationException e) {
                throw new OperationException(e);
            }
        }
    }

    /**
     * @since 5.9.2
     */
    protected Object resolveObject(final OperationContext ctx, final String key, Map<String, ?> args) {
        Object obj = args.get(key);
        if (obj != null) {
            return ctx.resolve(obj);
        }
        return ctx.getChainParameter(key);
    }

    /** @since 2021.17 */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(type).hashCode();
    }

    /** @since 2021.17 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OperationTypeImpl ot)) {
            return false;
        }
        return id.equals(ot.getId()) && type.equals(ot.type);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id)
                                        .append("type", type.getName())
                                        .append("params", params)
                                        .toString();
    }

    protected static Map<String, Field> initParams(Class<?> type) {
        var params = new HashMap<String, Field>();
        for (Field field : type.getDeclaredFields()) {
            Param param = field.getAnnotation(Param.class);
            if (param != null) {
                field.setAccessible(true);
                params.put(param.name(), field);
            }
        }
        return params;
    }

    protected static List<InvokableMethod> initMethods(OperationTypeImpl operationType, Class<?> type) {
        return Stream.of(type.getMethods())
                     // skip method that doesn't have OperationMethod annotation
                     .filter(method -> method.isAnnotationPresent(OperationMethod.class))
                     .<InvokableMethod> mapMulti((method, consumer) -> {
                         var annotation = method.getAnnotation(OperationMethod.class);
                         // register regular method
                         consumer.accept(new InvokableMethod(operationType, method, annotation));
                         // check for iterable input support
                         if (annotation.collector() != OutputCollector.class) {
                             // an iterable method - register it
                             consumer.accept(new InvokableIteratorMethod(operationType, method, annotation));
                         }
                     })
                     // method order depends on the JDK, make it deterministic
                     .sorted()
                     .toList();
    }

    protected static List<Field> initFields(Class<?> type) {
        var injectableFields = new ArrayList<Field>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Context.class)) {
                field.setAccessible(true);
                injectableFields.add(field);
            }
        }
        return injectableFields;
    }

    protected static class Match implements Comparable<Match> {
        protected InvokableMethod method;

        protected int priority;

        protected Match(InvokableMethod method, int priority) {
            this.method = method;
            this.priority = priority;
        }

        @Override
        public int compareTo(Match o) {
            return o.priority - priority;
        }

        @Override
        public String toString() {
            return "Match(" + method + ", " + priority + ")";
        }
    }
}
