/*
 * (C) Copyright 2010-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 5.6
 * @author Thierry Martins
 */
@XObject("veto")
public class NotificationListenerVetoDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<? extends NotificationListenerVeto> notificationVeto;

    @XNode("@remove")
    protected boolean remove = false;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Class<? extends NotificationListenerVeto> getNotificationVeto() {
        return notificationVeto;
    }

    public boolean isRemove() {
        return remove;
    }

    @Override
    public boolean doesRemove() {
        return remove;
    }
}
