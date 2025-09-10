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
package org.nuxeo.runtime.mockito;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.Mock;
import org.mockito.internal.configuration.FieldAnnotationProcessor;
import org.mockito.internal.configuration.IndependentAnnotationEngine;
import org.mockito.internal.configuration.InjectingAnnotationEngine;
import org.nuxeo.runtime.RuntimeServiceException;

/**
 * Used by file {@code mockito-extensions/org.mockito.plugins.AnnotationEngine}.
 *
 * @since 2025.0
 */
public class NuxeoAnnotationEngine extends InjectingAnnotationEngine {

    @SuppressWarnings("unchecked")
    public NuxeoAnnotationEngine() {
        super();
        try {
            // these classes are hard to subclass as they have many private methods
            // so instead we use reflection to set our NuxeoServiceMockAnnotationProcessor
            var delegate = (IndependentAnnotationEngine) FieldUtils.readField(this, "delegate", true);
            var annotationProcessorMap = (Map<Class<? extends Annotation>, FieldAnnotationProcessor<?>>) FieldUtils.readField(
                    delegate, "annotationProcessorMap", true);
            annotationProcessorMap.put(Mock.class, new NuxeoServiceMockAnnotationProcessor());
        } catch (IllegalAccessException e) {
            throw new RuntimeServiceException("Unable to configure Nuxeo Service Runtime Mockito bridge", e);
        }
    }
}
