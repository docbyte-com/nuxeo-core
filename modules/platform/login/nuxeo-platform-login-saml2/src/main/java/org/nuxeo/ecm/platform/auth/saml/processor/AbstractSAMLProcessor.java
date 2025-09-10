/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.auth.saml.processor;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.nuxeo.common.function.ThrowableConsumer;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.action.ProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.shared.component.InitializableComponent;
import net.shibboleth.shared.servlet.impl.HttpServletRequestResponseContext;

/**
 * @since 2023.0
 */
abstract class AbstractSAMLProcessor implements SAMLProcessor {

    /**
     * Prepares the OpenSAML context and run the actions on it.
     */
    @Override
    public final void execute(HttpServletRequest request, HttpServletResponse response) {
        // needed for decode/encode execution
        HttpServletRequestResponseContext.loadCurrent(request, response);

        var profileRequestContext = new ProfileRequestContext();
        profileRequestContext.setInboundMessageContext(new MessageContext());
        profileRequestContext.setOutboundMessageContext(new MessageContext());
        List<ProfileAction> actions = getActions();
        // init the actions
        actions.forEach(ThrowableConsumer.asConsumer(InitializableComponent::initialize));
        actions.forEach(a -> a.execute(profileRequestContext));
    }

    protected abstract List<ProfileAction> getActions();
}
