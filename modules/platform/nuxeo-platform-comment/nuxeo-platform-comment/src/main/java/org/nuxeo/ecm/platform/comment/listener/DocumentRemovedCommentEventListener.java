/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.comment.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.runtime.api.Framework;

public class DocumentRemovedCommentEventListener extends AbstractCommentListener implements PostCommitEventListener {

    private static final Logger log = LogManager.getLogger(DocumentRemovedCommentEventListener.class);

    @Override
    protected void doProcess(CoreSession coreSession, DocumentModel docMessage) {
        log.debug("Processing relations cleanup on Document removal");
        CommentManager commentManager = Framework.getService(CommentManager.class);
        deleteCommentChildren(coreSession, commentManager, docMessage);
        coreSession.save();
    }
}
