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
package org.nuxeo.ecm.webengine.app;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.UUID;

/**
 * @since 2025.0
 * @implNote it can't be a record, otherwise it breaks the WebEngine scan due to ASM not recent enough
 */
@JsonAutoDetect(fieldVisibility = PROTECTED_AND_PUBLIC)
public class MyObject {

    protected final String location;

    protected final String providerReference;

    protected final UUID uuid;

    protected MyObject(String location, int providerReference) {
        this(location, String.valueOf(providerReference));
    }

    protected MyObject(String location, String providerReference) {
        this.location = location;
        this.providerReference = providerReference;
        this.uuid = UUID.randomUUID();
    }

    public String location() {
        return location;
    }

    public String providerReference() {
        return providerReference;
    }
}
