/*
 * (C) Copyright 2024-2025 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.runtime.RuntimeServiceException;

/**
 * API that allows {@link RunnerFeature} to contribute dynamically a dependant {@link RunnerFeature} to the test
 * framework. This is accomplished by creating a constructor that has {@link DynamicFeaturesLoader} as parameter.
 * {@snippet :
 * public class MyFeature implements RunnerFeature {
 * 
 *     public MyFeature(DynamicFeaturesLoader loader) {
 *         loader.loadFeature(MyOtherFeature.class);
 *     }
 * }
 * }
 * 
 * @since 2025.0
 * @implNote This is a simple POJO that the internal loader will take into account when loading features into the test
 *           framework
 */
public class DynamicFeaturesLoader {

    protected final List<Class<? extends RunnerFeature>> features = new ArrayList<>();

    public void loadFeature(Class<? extends RunnerFeature> clazz) {
        this.features.add(clazz);
    }

    /**
     * @since 2025.4
     */
    public void loadFeature(String className) {
        this.features.add(getFeatureClass(className));
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends RunnerFeature> getFeatureClass(String className) {
        try {
            return (Class<? extends RunnerFeature>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeServiceException("The feature class: %s can not be loaded".formatted(className), e);
        }
    }
}
