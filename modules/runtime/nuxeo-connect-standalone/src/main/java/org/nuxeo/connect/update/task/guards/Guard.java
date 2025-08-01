/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.task.guards;

import java.util.Map;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.nuxeo.connect.update.PackageUpdateService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Guard {

    protected static final JxltEngine jexl = new JexlBuilder().permissions(JexlPermissions.UNRESTRICTED)
                                                              .create()
                                                              .createJxltEngine();

    protected final String value;

    protected JxltEngine.Expression expr;

    public Guard(String expr) {
        value = expr;
        this.expr = jexl.createExpression(expr);
    }

    public boolean evaluate(final Map<String, Object> map) {
        map.put("Version", new VersionHelper());
        map.put("Platform", new PlatformHelper());
        map.put("Packages", new PackagesHelper((PackageUpdateService) map.get("packageUpdateService")));
        JexlContext ctx = new MapContext(map);
        return (Boolean) expr.evaluate(ctx);
    }

}
