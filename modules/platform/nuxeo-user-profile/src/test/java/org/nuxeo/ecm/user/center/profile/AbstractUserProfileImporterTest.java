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
 */
package org.nuxeo.ecm.user.center.profile;

import java.io.File;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Before;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * @since 5.9.3
 */
public abstract class AbstractUserProfileImporterTest {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserProfileService userProfileService;

    @Inject
    protected UserManager userManager;

    @Before
    public void deleteAllUsers() {
        List<String> userIds = userManager.getUserIds();
        for (String userId : userIds) {
            NuxeoPrincipal principal = userManager.getPrincipal(userId);
            if (principal != null) {
                userManager.deleteUser(userId);
            }
        }
    }

    protected CoreSession openSession(NuxeoPrincipal principal) {
        return coreFeature.getCoreSession(principal);
    }

    protected NuxeoPrincipal createUser(String username, String tenant) {
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("user:username", username);
        user.setPropertyValue("user:tenantId", tenant);
        try {
            userManager.createUser(user);
        } catch (UserAlreadyExistsException e) {
            // do nothing
        } finally {
            session.save();
        }
        return userManager.getPrincipal(username);
    }

    protected NuxeoGroup createGroup(String groupName) {
        DocumentModel group = userManager.getBareGroupModel();
        group.setPropertyValue("group:groupname", groupName);
        String computedGroupName = groupName;
        try {
            computedGroupName = userManager.createGroup(group).getId();
        } finally {
            session.save();
        }
        return userManager.getGroup(computedGroupName);
    }

    protected File getBlobsFolder() {
        return new File(Framework.getProperty(UserProfileImporter.BLOB_FOLDER_PROPERTY));
    }

}
