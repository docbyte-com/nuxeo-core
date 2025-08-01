/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.VCSRepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Service holding the configuration for VCS repositories.
 *
 * @since 5.9.3
 */
public class SQLRepositoryService extends DefaultComponent {

    private static final String XP_REPOSITORY = "repository";

    @Override
    public void registerContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (contrib instanceof Descriptor descriptor) {
            register(XP_REPOSITORY, descriptor);
            updateRegistration(descriptor.getId());
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (contrib instanceof Descriptor descriptor) {
            unregister(XP_REPOSITORY, descriptor);
            updateRegistration(descriptor.getId());
        }
    }

    /**
     * Update repository registration in high-level repository service.
     */
    protected void updateRegistration(String repositoryName) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        RepositoryDescriptor descriptor = getDescriptor(XP_REPOSITORY, repositoryName);
        if (descriptor == null) {
            // last contribution removed
            repositoryManager.removeRepository(repositoryName);
            return;
        }
        // extract label, isDefault
        // and pass it to high-level registry
        RepositoryFactory repositoryFactory = new VCSRepositoryFactory(repositoryName);
        Repository repository = new Repository(repositoryName, descriptor.label, descriptor.isDefault(),
                descriptor.isHeadless(), repositoryFactory, descriptor.pool);
        repositoryManager.addRepository(repository);
    }

    public RepositoryDescriptor getRepositoryDescriptor(String name) {
        return getDescriptor(XP_REPOSITORY, name);
    }

    /**
     * Gets the list of SQL repository names.
     *
     * @return the list of SQL repository names
     * @since 5.9.5
     */
    public List<String> getRepositoryNames() {
        return getDescriptors(XP_REPOSITORY).stream().map(Descriptor::getId).collect(Collectors.toList());
    }

    /**
     * Gets the low-level SQL Repository of the given name.
     *
     * @param repositoryName the repository name
     * @return the repository
     * @since 5.9.5
     */
    public RepositoryManagement getRepository(String repositoryName) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        org.nuxeo.ecm.core.model.Repository repository = repositoryService.getRepository(repositoryName);
        if (repository == null) {
            throw new NuxeoException("Unknown repository: " + repositoryName);
        }
        if (repository instanceof org.nuxeo.ecm.core.storage.sql.Repository) {
            // (JCA) ConnectionFactoryImpl already implements Repository
            return (org.nuxeo.ecm.core.storage.sql.Repository) repository;
        } else {
            throw new NuxeoException("Unknown repository class: " + repository.getClass().getName());
        }
    }

    public RepositoryImpl getRepositoryImpl(String repositoryName) {
        return (RepositoryImpl) getRepository(repositoryName);
    }

    /**
     * Gets the repositories as a list of {@link RepositoryManagement} objects.
     *
     * @since 5.9.5
     * @return a list of {@link RepositoryManagement}
     */
    public List<RepositoryManagement> getRepositories() {
        List<RepositoryManagement> repositories = new ArrayList<>();
        for (String repositoryName : getRepositoryNames()) {
            repositories.add(getRepository(repositoryName));
        }
        return repositories;
    }

    public FulltextConfiguration getFulltextConfiguration(String repositoryName) {
        return getRepositoryImpl(repositoryName).getModel().getFulltextConfiguration();
    }

}
