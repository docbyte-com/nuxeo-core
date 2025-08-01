/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Versioning service component and implementation.
 */
public class VersioningComponent extends DefaultComponent implements VersioningService {

    public static final String VERSIONING_SERVICE_XP = "versioningService";

    public static final String VERSIONING_POLICY_XP = "policies";

    public static final String VERSIONING_FILTER_XP = "filters";

    public static final String VERSIONING_RESTRICTION_XP = "restrictions";

    protected static final StandardVersioningService STANDARD_VERSIONING_SERVICE = new StandardVersioningService();

    // public for tests
    public VersioningService service = null;

    protected ComponentContext context;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        this.context = context;
        this.service = STANDARD_VERSIONING_SERVICE;
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        this.context = null;
        this.service = null;
    }

    @Override
    public void registerContribution(Object contrib, String point, ComponentInstance contributor) {
        if (contrib instanceof VersioningPolicyDescriptor policy) {
            String componentName = contributor.getName().getName();
            if (policy.getOrder() <= 10 && !componentName.startsWith("org.nuxeo")) {
                throw new NuxeoException(
                        "Versioning policies with order lower or equal to 10 are reserved for internal purpose, "
                                + "please correct your policy with id: " + policy.getId() + " in component: "
                                + componentName);
            }
        }
        super.registerContribution(contrib, point, contributor);
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        recompute();
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        recompute();
    }

    protected void recompute() {
        var versioningService = defaultIfNull(instanciateVersioningService(), STANDARD_VERSIONING_SERVICE);
        if (versioningService instanceof ExtendableVersioningService evs) {
            evs.setVersioningPolicies(getSortedMappedDescriptors(VERSIONING_POLICY_XP));
            evs.setVersioningFilters(getMappedDescriptors(getDescriptors(VERSIONING_FILTER_XP)));
            evs.setVersioningRestrictions(getMappedDescriptors(getDescriptors(VERSIONING_RESTRICTION_XP)));
        }
        this.service = versioningService;
    }

    protected VersioningService instanciateVersioningService() {
        VersioningServiceDescriptor descriptor = this.<VersioningServiceDescriptor> getDescriptor(VERSIONING_SERVICE_XP,
                Descriptor.UNIQUE_DESCRIPTOR_ID);
        if (descriptor == null) {
            return null;
        }
        String klass = descriptor.className;
        try {
            return (VersioningService) context.getRuntimeContext()
                                              .loadClass(klass)
                                              .getDeclaredConstructor()
                                              .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Failed to instantiate: " + klass, e);
        }
    }

    protected <T extends Descriptor & Comparable<T>> LinkedHashMap<String, T> getSortedMappedDescriptors(String xp) {
        return this.<T> getMappedDescriptors(getDescriptors(xp))
                   .entrySet()
                   .stream()
                   .sorted(Map.Entry.comparingByValue())
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
                           LinkedHashMap::new));
    }

    protected <T extends Descriptor> Map<String, T> getMappedDescriptors(List<T> descriptors) {
        return descriptors.stream().collect(Collectors.toMap(Descriptor::getId, Function.identity()));
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return service.getVersionLabel(doc);
    }

    @Override
    public void doPostCreate(Document doc, Map<String, Serializable> options) {
        service.doPostCreate(doc, options);
    }

    @Override
    public List<VersioningOption> getSaveOptions(DocumentModel docModel) {
        return service.getSaveOptions(docModel);
    }

    @Override
    public boolean isPreSaveDoingCheckOut(Document doc, boolean isDirty, VersioningOption option,
            Map<String, Serializable> options) {
        return service.isPreSaveDoingCheckOut(doc, isDirty, option, options);
    }

    @Override
    public VersioningOption doPreSave(CoreSession session, Document doc, boolean isDirty, VersioningOption option,
            String checkinComment, Map<String, Serializable> options) {
        return service.doPreSave(session, doc, isDirty, option, checkinComment, options);
    }

    @Override
    public boolean isPostSaveDoingCheckIn(Document doc, VersioningOption option, Map<String, Serializable> options) {
        return service.isPostSaveDoingCheckIn(doc, option, options);
    }

    @Override
    public Document doPostSave(CoreSession session, Document doc, VersioningOption option, String checkinComment,
            Map<String, Serializable> options) {
        return service.doPostSave(session, doc, option, checkinComment, options);
    }

    @Override
    public Document doCheckIn(Document doc, VersioningOption option, String checkinComment) {
        return service.doCheckIn(doc, option, checkinComment);
    }

    @Override
    public void doCheckOut(Document doc) {
        service.doCheckOut(doc);
    }

    @Override
    public void doAutomaticVersioning(DocumentModel previousDocument, DocumentModel currentDocument, boolean before) {
        service.doAutomaticVersioning(previousDocument, currentDocument, before);
    }

}
