/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.NuxeoDriveContribException;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Default implementation of the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemItemAdapterServiceImpl extends DefaultComponent implements FileSystemItemAdapterService {

    private static final Logger log = LogManager.getLogger(FileSystemItemAdapterServiceImpl.class);

    public static final String FILE_SYSTEM_ITEM_FACTORY_EP = "fileSystemItemFactory";

    public static final String TOP_LEVEL_FOLDER_ITEM_FACTORY_EP = "topLevelFolderItemFactory";

    public static final String ACTIVE_FILE_SYSTEM_ITEM_FACTORIES_EP = "activeFileSystemItemFactories";

    protected static final String ACTIVE_FILE_SYSTEM_FOLDER_FACTORIES_SUB_EP = "activeTopLevelFolderItemFactoriesContrib";

    protected static final String CONCURRENT_SCROLL_BATCH_LIMIT = "org.nuxeo.drive.concurrentScrollBatchLimit";

    protected static final int CONCURRENT_SCROLL_BATCH_LIMIT_DEFAULT = 4;

    protected Set<String> activeFactories;

    protected Map<String, TopLevelFolderItemFactory> topLevelFolderItemFactories;

    protected TopLevelFolderItemFactory topLevelFolderItemFactory;

    protected List<FileSystemItemFactoryWrapper> fileSystemItemFactories;

    protected Semaphore scrollBatchSemaphore;

    /*------------------------ DefaultComponent -----------------------------*/
    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
            case FILE_SYSTEM_ITEM_FACTORY_EP -> {
                var descriptor = (FileSystemItemFactoryDescriptor) contribution;
                if (StringUtils.isEmpty(descriptor.getId())) {
                    throw new NuxeoException("Cannot register fileSystemItemFactory without a name.");
                }
                register(FILE_SYSTEM_ITEM_FACTORY_EP, descriptor);
            }
            case TOP_LEVEL_FOLDER_ITEM_FACTORY_EP -> {
                register(TOP_LEVEL_FOLDER_ITEM_FACTORY_EP, (Descriptor) contribution);
            }
            case ACTIVE_FILE_SYSTEM_ITEM_FACTORIES_EP -> {
                if (contribution instanceof ActiveTopLevelFolderItemFactoryDescriptor contrib) {
                    log.trace("Updating activeTopLevelFolderItemFactory contribution {}.", contrib);
                    log.trace("Setting active factory to {}.", contrib::getName);
                    register(ACTIVE_FILE_SYSTEM_FOLDER_FACTORIES_SUB_EP, contrib);
                } else if (contribution instanceof ActiveFileSystemItemFactoriesDescriptor contrib) {
                    register(ACTIVE_FILE_SYSTEM_ITEM_FACTORIES_EP, contrib);
                }
            }
            case null, default -> log.error("Unknown extension point {}", extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
            case FILE_SYSTEM_ITEM_FACTORY_EP -> unregister(FILE_SYSTEM_ITEM_FACTORY_EP, (Descriptor) contribution);
            case TOP_LEVEL_FOLDER_ITEM_FACTORY_EP -> {
                var contrib = (TopLevelFolderItemFactoryDescriptor) contribution;
                unregister(TOP_LEVEL_FOLDER_ITEM_FACTORY_EP, contrib);
            }
            case ACTIVE_FILE_SYSTEM_ITEM_FACTORIES_EP -> {
                if (contribution instanceof ActiveTopLevelFolderItemFactoryDescriptor contrib) {
                    unregister(ACTIVE_FILE_SYSTEM_FOLDER_FACTORIES_SUB_EP, contrib);
                } else if (contribution instanceof ActiveFileSystemItemFactoriesDescriptor descriptor) {
                    unregister(ACTIVE_FILE_SYSTEM_ITEM_FACTORIES_EP, descriptor);
                }
            }
            case null, default -> log.error("Unknown extension point {}", extensionPoint);
        }
    }

    protected void updateFileSystemFactories() {
        activeFactories = this.<ActiveFileSystemItemFactoriesDescriptor> getDescriptor(
                ACTIVE_FILE_SYSTEM_ITEM_FACTORIES_EP, UNIQUE_DESCRIPTOR_ID)
                              .getFactories()
                              .stream()
                              .filter(ActiveFileSystemItemFactoryDescriptor::isEnabled)
                              .map(ActiveFileSystemItemFactoryDescriptor::getName)
                              .collect(Collectors.toSet());
        fileSystemItemFactories = getOrderedActiveFactories();
    }

    protected List<FileSystemItemFactoryWrapper> getOrderedActiveFactories() {
        return this.<FileSystemItemFactoryDescriptor> getDescriptors(FILE_SYSTEM_ITEM_FACTORY_EP)
                   .stream()
                   .filter(f -> activeFactories.contains(f.getId()))
                   .sorted()
                   .map(f -> new FileSystemItemFactoryWrapper(f.getDocType(), f.getFacet(), f.getFactory()))
                   .collect(Collectors.toList());
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        topLevelFolderItemFactories = new HashMap<>();
        fileSystemItemFactories = new ArrayList<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        topLevelFolderItemFactories = null;
        fileSystemItemFactories = null;
    }

    /**
     * Sorts the contributed factories according to their order and initializes the {@link #scrollBatchSemaphore}.
     */
    @Override
    public void start(ComponentContext context) {
        initTopLevelFolderItemFactories();
        topLevelFolderItemFactory = topLevelFolderItemFactories.get(getActiveFolderItemFactory());
        updateFileSystemFactories();
        int concurrentScrollBatchLimit = Framework.getService(ConfigurationService.class)
                                                  .getInteger(CONCURRENT_SCROLL_BATCH_LIMIT,
                                                          CONCURRENT_SCROLL_BATCH_LIMIT_DEFAULT);
        scrollBatchSemaphore = new Semaphore(concurrentScrollBatchLimit, false);
    }

    @Override
    public void stop(ComponentContext context) {
        topLevelFolderItemFactories.clear();
        topLevelFolderItemFactory = null;
        activeFactories = null;
        fileSystemItemFactories = null;
        scrollBatchSemaphore = null;
    }

    protected void initTopLevelFolderItemFactories() {
        this.<TopLevelFolderItemFactoryDescriptor> getDescriptors(TOP_LEVEL_FOLDER_ITEM_FACTORY_EP).forEach(contrib -> {
            try {
                topLevelFolderItemFactories.put(contrib.getId(), contrib.getFactory());
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Cannot update topLevelFolderItemFactory contribution.", e);
            }
        });
    }

    protected String getActiveFolderItemFactory() {
        var descriptor = this.<ActiveTopLevelFolderItemFactoryDescriptor> getDescriptor(
                ACTIVE_FILE_SYSTEM_FOLDER_FACTORIES_SUB_EP, UNIQUE_DESCRIPTOR_ID);
        return descriptor != null ? descriptor.getName() : null;
    }

    /*------------------------ FileSystemItemAdapterService -----------------------*/
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc) {
        return getFileSystemItem(doc, false, null, false, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        return getFileSystemItem(doc, false, null, includeDeleted, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, false, null, includeDeleted, relaxSyncRootConstraint, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        return getFileSystemItem(doc, false, null, includeDeleted, relaxSyncRootConstraint, getLockInfo);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem) {
        return getFileSystemItem(doc, true, parentItem, false, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted) {
        return getFileSystemItem(doc, true, parentItem, includeDeleted, false, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, true, parentItem, includeDeleted, relaxSyncRootConstraint, true);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, FolderItem parentItem, boolean includeDeleted,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        return getFileSystemItem(doc, true, parentItem, includeDeleted, relaxSyncRootConstraint, getLockInfo);
    }

    /**
     * Iterates on the ordered contributed file system item factories until if finds one that can handle the given
     * {@link FileSystemItem} id.
     */
    @Override
    public FileSystemItemFactory getFileSystemItemFactoryForId(String id) {
        Iterator<FileSystemItemFactoryWrapper> factoriesIt = fileSystemItemFactories.iterator();
        while (factoriesIt.hasNext()) {
            FileSystemItemFactoryWrapper factoryWrapper = factoriesIt.next();
            FileSystemItemFactory factory = factoryWrapper.getFactory();
            if (factory.canHandleFileSystemItemId(id)) {
                return factory;
            }
        }
        // No fileSystemItemFactory found, try the topLevelFolderItemFactory
        if (getTopLevelFolderItemFactory().canHandleFileSystemItemId(id)) {
            return getTopLevelFolderItemFactory();
        }
        throw new NuxeoDriveContribException(String.format(
                "No fileSystemItemFactory found for FileSystemItem with id %s. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                id));
    }

    @Override
    public TopLevelFolderItemFactory getTopLevelFolderItemFactory() {
        if (topLevelFolderItemFactory == null) {
            throw new NuxeoDriveContribException(
                    "Found no active top level folder item factory. Please check there is a contribution to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"topLevelFolderItemFactory\"> and to <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"activeTopLevelFolderItemFactory\">.");
        }
        return topLevelFolderItemFactory;
    }

    @Override
    public VirtualFolderItemFactory getVirtualFolderItemFactory(String factoryName) {
        FileSystemItemFactory factory = getFileSystemItemFactory(factoryName);
        if (factory == null) {
            throw new NuxeoDriveContribException(String.format(
                    "No factory named %s. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\">.",
                    factoryName));
        }
        if (!(factory instanceof VirtualFolderItemFactory)) {
            throw new NuxeoDriveContribException(
                    String.format("Factory class %s for factory %s is not a VirtualFolderItemFactory.",
                            factory.getClass().getName(), factory.getName()));
        }
        return (VirtualFolderItemFactory) factory;
    }

    @Override
    public Set<String> getActiveFileSystemItemFactories() {
        if (activeFactories.isEmpty()) {
            throw new NuxeoDriveContribException(
                    "Found no active file system item factories. Please check there is a contribution to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"activeFileSystemItemFactories\"> declaring at least one factory.");
        }
        return activeFactories;
    }

    @Override
    public Semaphore getScrollBatchSemaphore() {
        return scrollBatchSemaphore;
    }

    /*------------------------- For test purpose ----------------------------------*/
    public Map<String, FileSystemItemFactoryDescriptor> getFileSystemItemFactoryDescriptors() {
        return this.<FileSystemItemFactoryDescriptor> getDescriptors(FILE_SYSTEM_ITEM_FACTORY_EP)
                   .stream()
                   .collect(Collectors.toMap(FileSystemItemFactoryDescriptor::getId, Function.identity()));
    }

    public List<FileSystemItemFactoryWrapper> getFileSystemItemFactories() {
        return fileSystemItemFactories;
    }

    public FileSystemItemFactory getFileSystemItemFactory(String name) {
        for (FileSystemItemFactoryWrapper factoryWrapper : fileSystemItemFactories) {
            FileSystemItemFactory factory = factoryWrapper.getFactory();
            if (name.equals(factory.getName())) {
                return factory;
            }
        }
        log.debug("No fileSystemItemFactory named {}, returning null.", name);
        return null;
    }

    /*--------------------------- Protected ---------------------------------------*/
    /**
     * Tries to adapt the given document as the top level {@link FolderItem}. If it doesn't match, iterates on the
     * ordered contributed file system item factories until it finds one that matches and retrieves a non null
     * {@link FileSystemItem} for the given document. A file system item factory matches if:
     * <ul>
     * <li>It is not bound to any docType nor facet (this is the case for the default factory contribution
     * {@code defaultFileSystemItemFactory} bound to {@link DefaultFileSystemItemFactory})</li>
     * <li>It is bound to a docType that matches the given doc's type</li>
     * <li>It is bound to a facet that matches one of the given doc's facets</li>
     * </ul>
     */
    protected FileSystemItem getFileSystemItem(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean includeDeleted, boolean relaxSyncRootConstraint, boolean getLockInfo) {

        FileSystemItem fileSystemItem;

        // Try the topLevelFolderItemFactory
        if (forceParentItem) {
            fileSystemItem = getTopLevelFolderItemFactory().getFileSystemItem(doc, parentItem, includeDeleted,
                    relaxSyncRootConstraint, getLockInfo);
        } else {
            fileSystemItem = getTopLevelFolderItemFactory().getFileSystemItem(doc, includeDeleted,
                    relaxSyncRootConstraint, getLockInfo);
        }
        if (fileSystemItem != null) {
            return fileSystemItem;
        } else {
            log.debug(
                    "The topLevelFolderItemFactory is not able to adapt document {} as a FileSystemItem => trying fileSystemItemFactories.",
                    doc::getId);
        }

        // Try the fileSystemItemFactories
        FileSystemItemFactoryWrapper matchingFactory = null;

        Iterator<FileSystemItemFactoryWrapper> factoriesIt = fileSystemItemFactories.iterator();
        while (factoriesIt.hasNext()) {
            FileSystemItemFactoryWrapper factory = factoriesIt.next();
            log.debug("Trying to adapt document {} (path: {}) as a FileSystemItem with factory {}", doc::getId,
                    doc::getPathAsString, () -> factory.getFactory().getName());
            if (generalFactoryMatches(factory) || docTypeFactoryMatches(factory, doc)
                    || facetFactoryMatches(factory, doc, relaxSyncRootConstraint)) {
                matchingFactory = factory;
                try {
                    if (forceParentItem) {
                        fileSystemItem = factory.getFactory()
                                                .getFileSystemItem(doc, parentItem, includeDeleted,
                                                        relaxSyncRootConstraint, getLockInfo);
                    } else {
                        fileSystemItem = factory.getFactory()
                                                .getFileSystemItem(doc, includeDeleted, relaxSyncRootConstraint,
                                                        getLockInfo);
                    }
                } catch (RootlessItemException e) {
                    // Give more information in the exception message on the
                    // document whose adaption failed to recursively find the
                    // top level item.
                    throw new RootlessItemException(String.format(
                            "Cannot find path to registered top" + " level when adapting document "
                                    + " '%s' (path: %s) with factory %s",
                            doc.getTitle(), doc.getPathAsString(), factory.getFactory().getName()), e);
                }
                if (fileSystemItem != null) {
                    log.debug("Adapted document '{}' (path: {}) to item with path {} with factory {}", doc::getTitle,
                            doc::getPathAsString, fileSystemItem::getPath, () -> factory.getFactory().getName());
                    return fileSystemItem;
                }
            }
        }

        if (matchingFactory == null) {
            log.debug(
                    "None of the fileSystemItemFactories matches document {} => returning null. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\">.",
                    doc::getId);
        } else {
            log.debug(
                    "None of the fileSystemItemFactories matching document {} were able to adapt this document as a FileSystemItem => returning null.",
                    doc::getId);
        }
        return fileSystemItem;
    }

    protected boolean generalFactoryMatches(FileSystemItemFactoryWrapper factory) {
        boolean matches = StringUtils.isEmpty(factory.getDocType()) && StringUtils.isEmpty(factory.getFacet());
        if (matches) {
            log.trace("General factory {} matches", factory);
        }
        return matches;
    }

    protected boolean docTypeFactoryMatches(FileSystemItemFactoryWrapper factory, DocumentModel doc) {
        boolean matches = !StringUtils.isEmpty(factory.getDocType()) && factory.getDocType().equals(doc.getType());
        if (matches) {
            log.trace("DocType factory {} matches for doc {} (path: {})", () -> factory, doc::getId,
                    doc::getPathAsString);
        }
        return matches;
    }

    protected boolean facetFactoryMatches(FileSystemItemFactoryWrapper factory, DocumentModel doc,
            boolean relaxSyncRootConstraint) {
        if (!StringUtils.isEmpty(factory.getFacet())) {
            for (String docFacet : doc.getFacets()) {
                if (factory.getFacet().equals(docFacet)) {
                    // Handle synchronization root case
                    if (NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET.equals(docFacet)) {
                        boolean matches = syncRootFactoryMatches(doc, relaxSyncRootConstraint);
                        if (matches) {
                            log.trace("Facet factory {} matches for doc {} (path: {})", () -> factory, doc::getId,
                                    doc::getPathAsString);
                        }
                        return matches;
                    } else {
                        log.trace("Facet factory {} matches for doc {} (path: {})", () -> factory, doc::getId,
                                doc::getPathAsString);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected boolean syncRootFactoryMatches(DocumentModel doc, boolean relaxSyncRootConstraint) {
        String userName = doc.getPrincipal().getName();
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) doc.getPropertyValue(
                NuxeoDriveManagerImpl.DRIVE_SUBSCRIPTIONS_PROPERTY);
        for (Map<String, Object> subscription : subscriptions) {
            if (Boolean.TRUE.equals(subscription.get("enabled"))) {
                if (userName.equals(subscription.get("username"))) {
                    log.trace("Doc {} (path: {}) registered as a sync root for user {}", doc::getId,
                            doc::getPathAsString, () -> userName);
                    return true;
                }
                if (relaxSyncRootConstraint) {
                    log.trace(
                            "Doc {} (path: {}) registered as a sync root for at least one user (relaxSyncRootConstraint is true)",
                            doc::getId, doc::getPathAsString);
                    return true;
                }
            }
        }
        return false;
    }

}
