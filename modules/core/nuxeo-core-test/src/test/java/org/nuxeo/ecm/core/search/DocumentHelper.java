/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import java.io.Serializable;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @since 2025.0
 */
public final class DocumentHelper {

    public static void addBlob(Property p, Blob blob) {
        if (p.isList()) {
            Type ft = ((ListProperty) p).getType().getFieldType();
            if (ft.isComplexType() && ((ComplexType) ft).getFieldsCount() == 1) {
                HashMap<String, Serializable> map = new HashMap<>();
                map.put("file", (Serializable) blob);
                p.addValue(map);
            } else {
                p.addValue(blob);
            }
        } else {
            p.setValue(blob);
        }
    }
}
