/*
 * (C) Copyright 2007-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
@XObject("settings")
public class GeneralSettingsDescriptor implements Descriptor {

    protected String serverPrefix;

    protected String eMailSubjectPrefix;

    protected String mailSessionJndiName;

    @XNode("mailSenderName")
    protected String mailSenderName;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    @XNode("serverPrefix")
    protected void setServerPrefix(String serverPrefix) {
        this.serverPrefix = Framework.expandVars(serverPrefix);
        if (this.serverPrefix != null) {
            this.serverPrefix = serverPrefix.endsWith("//") ? serverPrefix.substring(0, serverPrefix.length() - 1)
                    : serverPrefix;
        }
    }

    public String getEMailSubjectPrefix() {
        return eMailSubjectPrefix;
    }

    @XNode("eMailSubjectPrefix")
    protected void setEMailSubjectPrefix(String eMailSubjectPrefix) {
        this.eMailSubjectPrefix = Framework.expandVars(eMailSubjectPrefix);
    }

    public String getServerPrefix() {
        return serverPrefix;
    }

    /**
     * @deprecated since 2023.4 use {@link #getMailSenderName()} instead.
     */
    @Deprecated(since = "2023.4")
    public String getMailSessionJndiName() {
        return mailSessionJndiName;
    }

    @XNode("mailSessionJndiName")
    protected void setMailSessionJndiName(String mailSessionJndiName) {
        this.mailSessionJndiName = Framework.expandVars(mailSessionJndiName);
    }

    /**
     * Gets the name of the {@link org.nuxeo.mail.MailSender} to use.
     *
     * @since 2023.4
     */
    public String getMailSenderName() {
        return mailSenderName;
    }

}
