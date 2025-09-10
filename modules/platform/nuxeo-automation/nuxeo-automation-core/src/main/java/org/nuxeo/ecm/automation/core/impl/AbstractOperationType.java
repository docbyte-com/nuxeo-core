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
package org.nuxeo.ecm.automation.core.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;

/**
 * @since 2025.0
 */
public abstract class AbstractOperationType implements OperationType {

    protected List<String> buildSignature(List<InvokableMethod> methods,
            Function<InvokableMethod, Class<?>> outputTypeGetter) {
        var collectedSigs = new HashSet<String>();
        var signature = new ArrayList<String>(methods.size() * 2);
        for (var method : methods) {
            String input = getParamDocumentationType(method.getInputType(), method.isIterable());
            String output = getParamDocumentationType(outputTypeGetter.apply(method));
            String sigKey = input + ":" + output;
            if (!collectedSigs.contains(sigKey)) {
                signature.add(input);
                signature.add(output);
                collectedSigs.add(sigKey);
            }
        }
        return signature;
    }

    /**
     * @since 5.7.2
     */
    protected String getParamDocumentationType(Class<?> type) {
        return getParamDocumentationType(type, false);
    }

    /**
     * @since 5.7.2
     */
    protected String getParamDocumentationType(Class<?> type, boolean isIterable) {
        String t;
        if (DocumentModel.class.isAssignableFrom(type) || DocumentRef.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_DOCUMENTS : Constants.T_DOCUMENT;
        } else if (DocumentModelList.class.isAssignableFrom(type) || DocumentRefList.class.isAssignableFrom(type)) {
            t = Constants.T_DOCUMENTS;
        } else if (BlobList.class.isAssignableFrom(type)) {
            t = Constants.T_BLOBS;
        } else if (Blob.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_BLOBS : Constants.T_BLOB;
        } else if (URL.class.isAssignableFrom(type)) {
            t = Constants.T_RESOURCE;
        } else if (Calendar.class.isAssignableFrom(type)) {
            t = Constants.T_DATE;
        } else {
            t = type.getSimpleName().toLowerCase();
        }
        return t;
    }
}
