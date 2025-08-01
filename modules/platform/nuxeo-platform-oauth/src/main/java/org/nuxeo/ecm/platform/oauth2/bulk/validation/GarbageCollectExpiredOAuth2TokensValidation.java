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
package org.nuxeo.ecm.platform.oauth2.bulk.validation;

import static org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenServiceImpl.TOKEN_DIR;

import java.util.List;

import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;

/**
 * @since 2025.1
 */
public class GarbageCollectExpiredOAuth2TokensValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return List.of();
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {
        var query = SQLQueryParser.parse(command.getQuery());
        if (query.getFromClause().count() != 1) {
            throw new IllegalArgumentException("Invalid query:" + command.getQuery());
        }
        if (!TOKEN_DIR.equals(query.getFromClause().get(0))) {
            throw new IllegalArgumentException("Invalid directory name: " + query.getFromClause().get(0));
        }
    }
}
