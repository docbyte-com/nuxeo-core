/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.core.service;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutConverterDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutTypeDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetConverterDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetTypeDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.5
 */
public class LayoutStoreImpl extends DefaultComponent implements LayoutStore {

    private static final Logger log = LogManager.getLogger(LayoutStoreImpl.class);

    private static final long serialVersionUID = 1L;

    public static final String WIDGET_TYPES_EP_NAME = "widgettypes";

    /**
     * @since 6.0
     */
    public static final String LAYOUT_TYPES_EP_NAME = "layouttypes";

    public static final String WIDGETS_EP_NAME = "widgets";

    public static final String LAYOUTS_EP_NAME = "layouts";

    public static final String LAYOUT_CONVERTERS_EP_NAME = "layoutConverters";

    public static final String WIDGET_CONVERTERS_EP_NAME = "widgetConverters";

    protected final Map<String, Map<String, WidgetType>> widgetTypesByCat = new HashMap<>();

    protected final Map<String, Map<String, WidgetTypeDefinition>> widgetTypeDefsByCat = new HashMap<>();

    protected final Map<String, Map<String, LayoutTypeDefinition>> layoutTypeDefsByCat = new HashMap<>();

    protected final Map<String, Map<String, LayoutDefinition>> layoutsByCat = new HashMap<>();

    protected final Map<String, Map<String, WidgetDefinition>> widgetsByCat = new HashMap<>();

    protected Map<String, List<WidgetDefinitionConverter>> widgetConvertersByCat;

    protected Map<String, List<LayoutDefinitionConverter>> layoutConvertersByCat;

    // Runtime component API

    @Override
    public void start(ComponentContext context) {
        this.<WidgetTypeDescriptor> getDescriptors(WIDGET_TYPES_EP_NAME).forEach(descriptor -> {
            if (ArrayUtils.isEmpty(descriptor.getCategories())) {
                log.error("Cannot register widget type: {}, no category found", descriptor::getName);
            } else {
                for (String category : descriptor.getCategories()) {
                    registerWidgetType(category, descriptor.getWidgetTypeDefinition());
                }
            }
        });
        this.<LayoutTypeDescriptor> getDescriptors(LAYOUT_TYPES_EP_NAME).forEach(descriptor -> {
            if (ArrayUtils.isEmpty(descriptor.getCategories())) {
                log.error("Cannot register layout type: {}, no category found", descriptor::getName);
            } else {
                for (String category : descriptor.getCategories()) {
                    registerLayoutType(category, descriptor.getLayoutTypeDefinition());
                }
            }
        });
        this.<LayoutDescriptor> getDescriptors(LAYOUTS_EP_NAME).forEach(descriptor -> {
            if (ArrayUtils.isEmpty(descriptor.getCategories())) {
                log.error("Cannot register layout: {}, no category found", descriptor::getName);
            } else {
                for (String category : descriptor.getCategories()) {
                    registerLayout(category, descriptor.getLayoutDefinition());
                }
            }
        });
        this.<WidgetDescriptor> getDescriptors(WIDGETS_EP_NAME).forEach(descriptor -> {
            if (ArrayUtils.isEmpty(descriptor.getCategories())) {
                log.error("Cannot register widget: {}, no category found", descriptor::getName);
            } else {
                for (String category : descriptor.getCategories()) {
                    registerWidget(category, descriptor.getWidgetDefinition());
                }
            }
        });

        widgetConvertersByCat = this.<WidgetConverterDescriptor> getDescriptors(WIDGET_CONVERTERS_EP_NAME)
                                    .stream()
                                    .sorted()
                                    .<WidgetDefinitionConverterWithCategory> mapMulti(this::mapToConverter)
                                    .collect(Collectors.groupingBy(WidgetDefinitionConverterWithCategory::category,
                                            Collectors.mapping(WidgetDefinitionConverterWithCategory::converter,
                                                    Collectors.toList())));
        layoutConvertersByCat = this.<LayoutConverterDescriptor> getDescriptors(LAYOUT_CONVERTERS_EP_NAME)
                                    .stream()
                                    .sorted()
                                    .<LayoutDefinitionConverterWithCategory> mapMulti(this::mapToConverter)
                                    .collect(Collectors.groupingBy(LayoutDefinitionConverterWithCategory::category,
                                            Collectors.mapping(LayoutDefinitionConverterWithCategory::converter,
                                                    Collectors.toList())));
    }

    protected void mapToConverter(WidgetConverterDescriptor descriptor,
            Consumer<WidgetDefinitionConverterWithCategory> consumer) {
        if (ArrayUtils.isEmpty(descriptor.getCategories())) {
            log.error("Cannot instantiate widget converter: {}, no category found", descriptor::getName);
            return;
        }
        try {
            // instantiate the converter
            var converterClass = LayoutStoreImpl.class.getClassLoader().loadClass(descriptor.getConverterClassName());
            var converter = (WidgetDefinitionConverter) converterClass.getDeclaredConstructor().newInstance();
            // push elements to downstream for each category
            for (String category : descriptor.getCategories()) {
                consumer.accept(new WidgetDefinitionConverterWithCategory(category, converter));
            }
        } catch (ReflectiveOperationException e) {
            log.error("Can not instantiate widget converter with name: {}", descriptor.getName(), e);
        }
    }

    protected void mapToConverter(LayoutConverterDescriptor descriptor,
            Consumer<LayoutDefinitionConverterWithCategory> consumer) {
        if (ArrayUtils.isEmpty(descriptor.getCategories())) {
            log.error("Cannot instantiate layout converter: {}, no category found", descriptor::getName);
            return;
        }
        try {
            // instantiate the converter
            var converterClass = LayoutStoreImpl.class.getClassLoader().loadClass(descriptor.getConverterClassName());
            var converter = (LayoutDefinitionConverter) converterClass.getDeclaredConstructor().newInstance();
            // push elements to downstream for each category
            for (String category : descriptor.getCategories()) {
                consumer.accept(new LayoutDefinitionConverterWithCategory(category, converter));
            }
        } catch (ReflectiveOperationException e) {
            log.error("Can not instantiate layout converter with name: {}", descriptor.getName(), e);
        }
    }

    @Override
    public void stop(ComponentContext context) {
        widgetTypesByCat.clear();
        widgetTypeDefsByCat.clear();
        layoutTypeDefsByCat.clear();
        layoutsByCat.clear();
        widgetsByCat.clear();
        widgetConvertersByCat = null;
        layoutConvertersByCat = null;
    }

    // Categories

    @Override
    public List<String> getCategories() {
        Set<String> cats = new HashSet<>();
        cats.addAll(widgetTypeDefsByCat.keySet());
        cats.addAll(widgetTypesByCat.keySet());
        cats.addAll(layoutsByCat.keySet());
        cats.addAll(widgetsByCat.keySet());
        List<String> res = new ArrayList<>(cats);
        Collections.sort(res);
        return res;
    }

    // widget types

    @Override
    public void registerWidgetType(String category, WidgetTypeDefinition definition) {
        String name = definition.getName();
        var names = new ArrayList<>(List.of(name));
        names.addAll(emptyIfNull(definition.getAliases()));
        String className = definition.getHandlerClassName();
        Class<?> widgetTypeClass = null;
        if (className != null) {
            try {
                widgetTypeClass = LayoutStoreImpl.class.getClassLoader().loadClass(className);
            } catch (ReflectiveOperationException e) {
                log.error("Caught error when instantiating widget type handler", e);
                return;
            }
        }
        var widgetType = new WidgetTypeImpl(name, widgetTypeClass, definition.getProperties());
        widgetType.setAliases(definition.getAliases());

        var widgetTypesByName = widgetTypesByCat.computeIfAbsent(category, k -> new HashMap<>());
        names.forEach(n -> widgetTypesByName.put(n, widgetType));

        var widgetTypeDefsByName = widgetTypeDefsByCat.computeIfAbsent(category, k -> new HashMap<>());
        names.forEach(n -> widgetTypeDefsByName.put(n, definition));
        log.info("Registered widget type: {} for category: {}", name, category);
    }

    @Override
    public void unregisterWidgetType(String category, WidgetTypeDefinition definition) {
        String name = definition.getName();
        var names = new ArrayList<>(List.of(name));
        names.addAll(emptyIfNull(definition.getAliases()));
        // the method didn't remove WidgetType before the fragment registry removal because the equals method were used
        // in registries to identify the object to remove while we don't give the same object (for equals method)
        var widgetTypesByName = widgetTypesByCat.getOrDefault(category, Map.of());
        names.forEach(widgetTypesByName::remove);
        var widgetTypeDefsByName = widgetTypeDefsByCat.getOrDefault(category, Map.of());
        names.forEach(widgetTypeDefsByName::remove);
        log.info("Unregistered widget type: {} for category: {}", name, category);
    }

    // layout types

    @Override
    public void registerLayoutType(String category, LayoutTypeDefinition layoutTypeDef) {
        var layoutTypeDefsByName = layoutTypeDefsByCat.computeIfAbsent(category, k -> new HashMap<>());
        layoutTypeDefsByName.put(layoutTypeDef.getName(), layoutTypeDef);
        emptyIfNull(layoutTypeDef.getAliases()).forEach(alias -> layoutTypeDefsByName.put(alias, layoutTypeDef));
        log.info("Registered layout type: {} for category: {}", layoutTypeDef.getName(), category);
    }

    @Override
    public void unregisterLayoutType(String category, LayoutTypeDefinition layoutTypeDef) {
        var layoutTypeDefsByName = layoutTypeDefsByCat.getOrDefault(category, Map.of());
        layoutTypeDefsByName.remove(layoutTypeDef.getName());
        emptyIfNull(layoutTypeDef.getAliases()).forEach(layoutTypeDefsByName::remove);
        log.info("Unregistered layout type: {} for category: {}", layoutTypeDef.getName(), category);
    }

    // layouts

    @Override
    public void registerLayout(String category, LayoutDefinition layoutDef) {
        var layoutsByName = layoutsByCat.computeIfAbsent(category, k -> new HashMap<>());
        layoutsByName.put(layoutDef.getName(), layoutDef);
        emptyIfNull(layoutDef.getAliases()).forEach(alias -> layoutsByName.put(alias, layoutDef));
        log.info("Registered layout: {} for category: {}", layoutDef.getName(), category);
    }

    @Override
    public void unregisterLayout(String category, LayoutDefinition layoutDef) {
        var layoutsByName = layoutsByCat.getOrDefault(category, Map.of());
        layoutsByName.remove(layoutDef.getName());
        emptyIfNull(layoutDef.getAliases()).forEach(layoutsByName::remove);
        log.info("Unregistered layout: {} for category: {}", layoutDef.getName(), category);
    }

    // widgets

    @Override
    public void registerWidget(String category, WidgetDefinition widgetDef) {
        var widgetsByName = widgetsByCat.computeIfAbsent(category, k -> new HashMap<>());
        widgetsByName.put(widgetDef.getName(), widgetDef);
        emptyIfNull(widgetDef.getAliases()).forEach(alias -> widgetsByName.put(alias, widgetDef));
        log.info("Registered widget: {} for category: {}", widgetDef.getName(), category);
    }

    @Override
    public void unregisterWidget(String category, WidgetDefinition widgetDef) {
        var widgetsByName = widgetsByCat.getOrDefault(category, Map.of());
        widgetsByName.remove(widgetDef.getName());
        emptyIfNull(widgetDef.getAliases()).forEach(widgetsByName::remove);
        log.info("Unregistered widget: {} for category: {}", widgetDef.getName(), category);
    }

    // service api

    @Override
    public WidgetType getWidgetType(String category, String typeName) {
        return widgetTypesByCat.getOrDefault(category, Map.of()).get(typeName);
    }

    @Override
    public WidgetTypeDefinition getWidgetTypeDefinition(String category, String typeName) {
        return widgetTypeDefsByCat.getOrDefault(category, Map.of()).get(typeName);
    }

    @Override
    public List<WidgetTypeDefinition> getWidgetTypeDefinitions(String category) {
        return new ArrayList<>(widgetTypeDefsByCat.getOrDefault(category, Map.of()).values());
    }

    @Override
    public LayoutTypeDefinition getLayoutTypeDefinition(String category, String typeName) {
        return layoutTypeDefsByCat.getOrDefault(category, Map.of()).get(typeName);
    }

    @Override
    public List<LayoutTypeDefinition> getLayoutTypeDefinitions(String category) {
        return new ArrayList<>(layoutTypeDefsByCat.getOrDefault(category, Map.of()).values());
    }

    @Override
    public LayoutDefinition getLayoutDefinition(String category, String layoutName) {
        return layoutsByCat.getOrDefault(category, Map.of()).get(layoutName);
    }

    @Override
    public List<String> getLayoutDefinitionNames(String category) {
        return new ArrayList<>(layoutsByCat.getOrDefault(category, Map.of()).keySet());
    }

    @Override
    public WidgetDefinition getWidgetDefinition(String category, String widgetName) {
        return widgetsByCat.getOrDefault(category, Map.of()).get(widgetName);
    }

    @Override
    public List<LayoutDefinitionConverter> getLayoutConverters(String category) {
        return layoutConvertersByCat.getOrDefault(category, List.of());
    }

    @Override
    public List<WidgetDefinitionConverter> getWidgetConverters(String category) {
        return widgetConvertersByCat.getOrDefault(category, List.of());
    }

    protected record WidgetDefinitionConverterWithCategory(String category, WidgetDefinitionConverter converter) {
    }

    protected record LayoutDefinitionConverterWithCategory(String category, LayoutDefinitionConverter converter) {
    }
}
