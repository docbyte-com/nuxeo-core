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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.search.index.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.index.commands.IndexingCommand.Type;

public class TestIndexingCommand {

    @Test
    public void testConstructorOk() {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, false);
        cmd = new IndexingCommand(doc, Type.INSERT, true, false);
        assertTrue(cmd.isSync());
        assertFalse(cmd.isRecurse());
        cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        // delete recurse and sync is accepted
        cmd = new IndexingCommand(doc, Type.DELETE, true, true);
        assertTrue(cmd.isSync());
        assertTrue(cmd.isRecurse());

    }

    @Test
    public void testConstructorWithRecurseSync() {
        DocumentModel doc = new MockDocumentModel("foo");
        assertThrows(IllegalArgumentException.class, () -> new IndexingCommand(doc, Type.INSERT, true, true));
    }

    @Test
    public void testConstructorWithNullDoc() {
        assertThrows(IllegalArgumentException.class, () -> new IndexingCommand(null, Type.INSERT, true, false));
    }

    @Test
    public void testConstructorWithNullDocId() {
        DocumentModel doc = new MockDocumentModel(null);
        assertThrows(IllegalArgumentException.class, () -> new IndexingCommand(doc, Type.INSERT, true, false));
    }

    @Test
    public void testAddSchemas() {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, true, false);
        assertNull(cmd.getSchemas());
        cmd.addSchemas("mySchema");
        assertEquals(1, cmd.getSchemas().length);
    }

    @Test
    public void testMakeSync() {
        DocumentModel doc = new MockDocumentModel("foo");
        // ok for non recursive command
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, false);
        cmd.makeSync();
        assertTrue(cmd.isSync());
        // recursive command cannot be turned into sync
        cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        cmd.makeSync();
        assertFalse(cmd.isSync());
        // except for deletion
        cmd = new IndexingCommand(doc, Type.DELETE, false, true);
        cmd.makeSync();
        assertTrue(cmd.isSync());
    }

    @Test
    public void testJson() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        String json = cmd.toJSON();
        IndexingCommand cmd2 = IndexingCommand.fromJSON(json);
        String json2 = cmd2.toJSON();
        assertEquals(json, json2);
        assertTrue(cmd2.isRecurse());
    }

    @Test
    public void testInvalidJson() throws Exception {
        DocumentModel doc = new MockDocumentModel("foo");
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, true);
        String json = "{" + cmd.toJSON();
        assertThrows(IllegalArgumentException.class, () -> IndexingCommand.fromJSON(json));
    }

    @Test
    public void testInvalidJsonDocIdNull() {
        String json = "{\"id\": \"124\", \"type\": \"INSERT\"}";
        assertThrows(IllegalArgumentException.class, () -> IndexingCommand.fromJSON(json));
    }
}
