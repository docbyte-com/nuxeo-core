/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.oauth2.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

/**
 * @since 2025.1
 */
public class GarbageCollectExpiredOAuth2TokensListener implements PostCommitEventListener {

    private static final Logger log = LogManager.getLogger(GarbageCollectExpiredOAuth2TokensListener.class);

    public static final String EVENT_NAME = "garbageCollectExpiredOAuth2Tokens";

    @Override
    public void handleEvent(EventBundle events) {
        log.debug("Triggering expired OAuth2 tokens garbage collection");
        for (Event event : events) {
            if (EVENT_NAME.equals(event.getName())) {
                try (NuxeoLoginContext loginContext = Framework.loginSystem()) {
                    Framework.getService(OAuth2TokenService.class).garbageCollectExpiredTokens();
                }
            }
        }
    }
}
