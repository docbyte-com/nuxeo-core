/*
 * (C) Copyright 2014-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.search.index.commands;

import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;

/*
 * This class holds a list of indexing commands and manages de-duplication
 */
public class IndexingCommands {

    protected final List<IndexingCommand> commands = new ArrayList<>();

    protected final Set<IndexingCommand.Type> commandTypes = new HashSet<>();

    protected DocumentModel targetDocument;

    protected IndexingCommands() {
        //
    }

    public IndexingCommands(DocumentModel targetDocument) {
        this.targetDocument = targetDocument;
    }

    public void add(IndexingCommand.Type type, boolean sync, boolean recurse) {
        IndexingCommand cmd = new IndexingCommand(targetDocument, type, sync, recurse);
        add(cmd);
    }

    protected IndexingCommand find(IndexingCommand.Type command) {
        for (IndexingCommand cmd : commands) {
            if (cmd.type == command) {
                return cmd;
            }
        }
        return null;
    }

    protected void add(IndexingCommand command) {
        if (command == null) {
            return;
        }
        if (commandTypes.contains(command.type)) {
            IndexingCommand existing = find(command.type);
            if (existing.merge(command)) {
                return;
            }
        } else if (commandTypes.contains(IndexingCommand.Type.INSERT)
                || commandTypes.contains(IndexingCommand.Type.UPDATE)) {
            if (command.type == IndexingCommand.Type.DELETE) {
                // index and delete in the same tx
                clear();
            } else if (command.isSync()) {
                IndexingCommand existing = requireNonNullElse(find(IndexingCommand.Type.INSERT),
                        find(IndexingCommand.Type.UPDATE));
                // switch to sync if possible
                existing.makeSync();
            }
            // we already have an index command, don't care about the new command
            return;
        }
        if (command.type == IndexingCommand.Type.DELETE) {
            // no need to keep event before delete.
            clear();
        }
        commands.add(command);
        commandTypes.add(command.type);
    }

    protected void clear() {
        commands.clear();
        commandTypes.clear();
    }

    public DocumentModel getTargetDocument() {
        return targetDocument;
    }

    public boolean contains(IndexingCommand.Type command) {
        return commandTypes.contains(command);
    }

    public List<IndexingCommand> getCommands() {
        return commands;
    }

}
