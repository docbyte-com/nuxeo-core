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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.types;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfiguration;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

public class TypeService extends DefaultComponent implements TypeManager {

    public static final ComponentName ID = new ComponentName("org.nuxeo.ecm.platform.types.TypeService");

    public static final String DEFAULT_CATEGORY = "misc";

    public static final String HIDDEN_IN_CREATION = "create";

    protected Map<String, Type> types;

    protected Map<String, DocumentTypeDescriptor> documentTypes;

    private Runnable recomputeCallback;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) {
    }

    @Override
    public void start(ComponentContext context) {
        types = this.<Type> getDescriptors("types")
                    .stream()
                    // make a copy of the given Type in case of there's only one Descriptor, and so no merge happened
                    // this will prevent to modify the original descriptor within recomputeTypes and breaks hot reload
                    .map(type -> type.merge(new Type()))
                    .collect(Collectors.toMap(Type::getId, Function.identity()));
        documentTypes = types.values().stream().map(type -> {
            var documentType = new DocumentTypeDescriptor();
            documentType.name = type.getId();
            documentType.subtypes = type.getAllowedSubTypes().keySet().toArray(String[]::new);
            documentType.forbiddenSubtypes = type.getDeniedSubTypes();
            documentType.append = true;
            return documentType;
        })
                             .filter(descriptor -> isNotEmpty(descriptor.subtypes)
                                     || isNotEmpty(descriptor.forbiddenSubtypes))
                             .collect(Collectors.toMap(descriptor -> descriptor.name, Function.identity()));

        // register documentTypes to SchemaManager
        var schemaManager = (SchemaManagerImpl) Framework.getService(SchemaManager.class);
        documentTypes.values().forEach(schemaManager::registerDocumentType);
        // recompute types from SchemaManager
        recomputeTypes();
        // register recompute types callback
        recomputeCallback = this::recomputeTypes;
        schemaManager.registerRecomputeCallback(recomputeCallback);
    }

    @Override
    public void stop(ComponentContext context) {
        var schemaManager = (SchemaManagerImpl) Framework.getService(SchemaManager.class);
        documentTypes.values().forEach(schemaManager::unregisterDocumentType);
        schemaManager.unregisterRecomputeCallback(recomputeCallback);

        types = null;
        documentTypes = null;
    }

    /**
     * @since 8.10
     */
    protected void recomputeTypes() {
        for (Type type : types.values()) {
            type.setAllowedSubTypes(getCoreAllowedSubtypes(type));
            // do not need to add denied subtypes because allowed subtypes already come filtered from core
            type.setDeniedSubTypes(new String[0]);
        }
    }

    /**
     * @since 8.10
     */
    protected Map<String, SubType> getCoreAllowedSubtypes(Type type) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Collection<String> coreAllowedSubtypes = schemaManager.getAllowedSubTypes(type.getId());
        if (coreAllowedSubtypes == null) {
            // there are no subtypes to take care of
            return Map.of();
        }

        Map<String, SubType> ecmSubTypes = type.getAllowedSubTypes();
        Map<String, SubType> allowedSubTypes = new HashMap<>();
        SubType subtype;
        for (String name : coreAllowedSubtypes) {
            if (ecmSubTypes.containsKey(name)) {
                subtype = ecmSubTypes.get(name);
            } else {
                subtype = new SubType();
                subtype.setName(name);
            }
            allowedSubTypes.put(name, subtype);
        }

        return allowedSubTypes;
    }

    // Service implementation for TypeManager interface

    @Override
    public String[] getSuperTypes(String typeName) {
        SchemaManager schemaMgr = Framework.getService(SchemaManager.class);
        DocumentType type = schemaMgr.getDocumentType(typeName);
        if (type == null) {
            return null;
        }
        type = (DocumentType) type.getSuperType();
        List<String> superTypes = new ArrayList<>();
        while (type != null) {
            superTypes.add(type.getName());
            type = (DocumentType) type.getSuperType();
        }
        return superTypes.toArray(String[]::new);
    }

    @Override
    public Type getType(String typeName) {
        return types.get(typeName);
    }

    @Override
    public boolean hasType(String typeName) {
        return types.containsKey(typeName);
    }

    @Override
    public Collection<Type> getTypes() {
        return new ArrayList<>(types.values());
    }

    @Override
    public Collection<Type> getAllowedSubTypes(String typeName) {
        return getAllowedSubTypes(typeName, null);
    }

    @Override
    public Collection<Type> getAllowedSubTypes(String typeName, DocumentModel currentDoc) {
        Collection<Type> allowed = new ArrayList<>();
        Type type = getType(typeName);
        if (type != null) {
            Map<String, SubType> allowedSubTypes = type.getAllowedSubTypes();
            if (currentDoc != null) {
                allowedSubTypes = filterSubTypesFromConfiguration(allowedSubTypes, currentDoc);
            }
            for (String subTypeName : allowedSubTypes.keySet()) {
                Type subType = getType(subTypeName);
                if (subType != null) {
                    allowed.add(subType);
                }
            }
        }
        return allowed;
    }

    @Override
    public Collection<Type> findAllAllowedSubTypesFrom(String typeName) {
        return findAllAllowedSubTypesFrom(typeName, null, null);
    }

    @Override
    public Collection<Type> findAllAllowedSubTypesFrom(String typeName, DocumentModel currentDoc) {
        return findAllAllowedSubTypesFrom(typeName, currentDoc, null);
    }

    protected Collection<Type> findAllAllowedSubTypesFrom(String typeName, DocumentModel currentDoc,
            List<String> alreadyProcessedTypes) {
        if (alreadyProcessedTypes == null) {
            alreadyProcessedTypes = new ArrayList<>();
        }

        Collection<Type> allowedSubTypes = getAllowedSubTypes(typeName, currentDoc);
        Set<Type> allAllowedSubTypes = new HashSet<>(allowedSubTypes);
        alreadyProcessedTypes.add(typeName);
        for (Type subType : allowedSubTypes) {
            if (!alreadyProcessedTypes.contains(subType.getId())) {
                allAllowedSubTypes.addAll(
                        findAllAllowedSubTypesFrom(subType.getId(), currentDoc, alreadyProcessedTypes));
            }
        }

        return allAllowedSubTypes;
    }

    protected UITypesConfiguration getConfiguration(DocumentModel currentDoc) {
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        return localConfigurationService.getConfiguration(UITypesConfiguration.class, UI_TYPES_CONFIGURATION_FACET,
                currentDoc);
    }

    @Override
    public Map<String, List<Type>> getTypeMapForDocumentType(String typeName, DocumentModel currentDoc) {
        Type type = getType(typeName);
        if (type != null) {
            Map<String, List<Type>> docTypesMap = new HashMap<>();
            Map<String, SubType> allowedSubTypes = type.getAllowedSubTypes();
            allowedSubTypes = filterSubTypesFromConfiguration(allowedSubTypes, currentDoc);
            for (Map.Entry<String, SubType> entry : allowedSubTypes.entrySet()) {
                if (canCreate(entry.getValue())) {
                    Type subType = getType(entry.getKey());
                    if (subType != null) {
                        String key = subType.getCategory();
                        if (key == null) {
                            key = DEFAULT_CATEGORY;
                        }
                        if (!docTypesMap.containsKey(key)) {
                            docTypesMap.put(key, new ArrayList<Type>());
                        }
                        docTypesMap.get(key).add(subType);
                    }
                }
            }
            return docTypesMap;
        }
        return new HashMap<>();
    }

    @Override
    public boolean canCreate(String typeName, String containerTypeName) {
        Type containerType = getType(containerTypeName);
        Map<String, SubType> allowedSubTypes = containerType.getAllowedSubTypes();
        return canCreate(typeName, allowedSubTypes);
    }

    @Override
    public boolean canCreate(String typeName, String containerTypeName, DocumentModel currentDoc) {
        Map<String, SubType> allowedSubTypes = getFilteredAllowedSubTypes(containerTypeName, currentDoc);
        return canCreate(typeName, allowedSubTypes);
    }

    protected Map<String, SubType> getFilteredAllowedSubTypes(String containerTypeName, DocumentModel currentDoc) {
        Type containerType = getType(containerTypeName);
        if (containerType == null) {
            return Map.of();
        }
        Map<String, SubType> allowedSubTypes = containerType.getAllowedSubTypes();
        return filterSubTypesFromConfiguration(allowedSubTypes, currentDoc);
    }

    protected boolean canCreate(String typeName, Map<String, SubType> allowedSubTypes) {
        if (!isAllowedSubType(typeName, allowedSubTypes)) {
            return false;
        }

        SubType subType = allowedSubTypes.get(typeName);
        return canCreate(subType);
    }

    protected boolean canCreate(SubType subType) {
        List<String> hidden = subType.getHidden();
        return !(hidden != null && hidden.contains(HIDDEN_IN_CREATION));
    }

    @Override
    public boolean isAllowedSubType(String typeName, String containerTypeName) {
        Type containerType = getType(containerTypeName);
        if (containerType == null) {
            return false;
        }
        Map<String, SubType> allowedSubTypes = containerType.getAllowedSubTypes();
        return isAllowedSubType(typeName, allowedSubTypes);
    }

    protected boolean isAllowedSubType(String typeName, Map<String, SubType> allowedSubTypes) {
        for (String subTypeName : allowedSubTypes.keySet()) {
            if (subTypeName.equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAllowedSubType(String typeName, String containerTypeName, DocumentModel currentDoc) {
        Map<String, SubType> allowedSubTypes = getFilteredAllowedSubTypes(containerTypeName, currentDoc);
        return isAllowedSubType(typeName, allowedSubTypes);
    }

    protected Map<String, SubType> filterSubTypesFromConfiguration(Map<String, SubType> allowedSubTypes,
            DocumentModel currentDoc) {
        UITypesConfiguration uiTypesConfiguration = getConfiguration(currentDoc);
        if (uiTypesConfiguration != null) {
            allowedSubTypes = uiTypesConfiguration.filterSubTypes(allowedSubTypes);
        }
        return allowedSubTypes;
    }

}
