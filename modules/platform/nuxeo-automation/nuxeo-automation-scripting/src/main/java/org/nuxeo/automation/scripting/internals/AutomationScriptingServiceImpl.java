/*
 * (C) Copyright 2016-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.automation.scripting.internals;

import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.AUTOMATION_SCRIPTING_PRECOMPILE;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.DEFAULT_PRECOMPILE_STATUS;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.openjdk.nashorn.api.scripting.ClassFilter;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

public class AutomationScriptingServiceImpl implements AutomationScriptingService {

    /** @since 2023.9 */
    public static final String OPTIMISTIC_TYPES_ENABLED_PROPERTY_KEY = "nuxeo.automation.scripting.optimistic.types.enabled";

    protected final ScriptEngine engine = getScriptEngine();

    protected volatile CompiledScript mapperScript;

    // updated in-place only by extension points, so no concurrency issues
    protected final Set<String> allowedClassNames;

    public AutomationScriptingServiceImpl(Set<String> allowedClassNames) {
        this.allowedClassNames = allowedClassNames;
    }

    @Override
    public Session get(CoreSession session) {
        return get(new OperationContext(session));
    }

    @Override
    public Session get(OperationContext context) {
        return new Bridge(context);
    }

    protected CompiledScript getMapperScript() {
        if (mapperScript == null) {
            synchronized (this) {
                if (mapperScript == null) {
                    mapperScript = AutomationMapper.compile((Compilable) engine);
                }
            }
        }
        return mapperScript;
    }

    class Bridge implements Session {

        final Invocable invocable = ((Invocable) engine);

        final ScriptContext scriptContext = engine.getContext();

        final AutomationMapper mapper;

        final ScriptObjectMirror global;

        Bridge(OperationContext operationContext) {
            mapper = new AutomationMapper(operationContext);
            try {
                getMapperScript().eval(mapper);
            } catch (ScriptException cause) {
                throw new NuxeoException("Cannot execute mapper " + mapperScript, cause);
            }
            global = (ScriptObjectMirror) mapper.get("nashorn.global");
            scriptContext.setBindings(mapper, ScriptContext.ENGINE_SCOPE);
        }

        @Override
        public <T> T handleof(InputStream input, Class<T> typeof) {
            run(input);
            T handle = invocable.getInterface(global, typeof);
            if (handle == null) {
                throw new NuxeoException("Script doesn't implements " + typeof.getName());
            }
            return typeof.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[] { typeof }, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return mapper.unwrap(method.invoke(handle, mapper.wrap(args[0]), mapper.wrap(args[1])));
                        }
                    }));
        }

        @Override
        public Object run(InputStream input) {
            try {
                return mapper.unwrap(engine.eval(new InputStreamReader(input), mapper));
            } catch (ScriptException cause) {
                throw new NuxeoException("Cannot evaluate automation script", cause);
            }
        }

        @Override
        public <T> T adapt(Class<T> typeof) {
            if (typeof.isAssignableFrom(engine.getClass())) {
                return typeof.cast(engine);
            }
            if (typeof.isAssignableFrom(AutomationMapper.class)) {
                return typeof.cast(mapper);
            }
            if (typeof.isAssignableFrom(scriptContext.getClass())) {
                return typeof.cast(scriptContext);
            }
            throw new IllegalArgumentException("Cannot adapt scripting context to " + typeof.getName());
        }

        @Override
        public void close() throws Exception {
            mapper.flush();
        }
    }

    protected ScriptEngine getScriptEngine() {
        boolean cache = Boolean.parseBoolean(
                Framework.getProperty(AUTOMATION_SCRIPTING_PRECOMPILE, DEFAULT_PRECOMPILE_STATUS));
        return getScriptEngine(cache);
    }

    protected ScriptEngine getScriptEngine(boolean cache) {
        NashornScriptEngineFactory nashorn = new NashornScriptEngineFactory();
        String[] args = cache
                ? new String[] { "-strict", "--optimistic-types=" + isOptimisticTypesEnabled(),
                        "--persistent-code-cache", "--class-cache-size=50" }
                : new String[] { "-strict" };
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return nashorn.getScriptEngine(args, classLoader, getClassFilter());
    }

    protected boolean isOptimisticTypesEnabled() {
        return Boolean.parseBoolean(Framework.getProperty(OPTIMISTIC_TYPES_ENABLED_PROPERTY_KEY));
    }

    @SuppressWarnings("Convert2MethodRef")
    protected ClassFilter getClassFilter() {
        return className -> allowedClassNames.contains(className);
    }

}
