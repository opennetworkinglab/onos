/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.store.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * Unit tests for {@link DocumentPath}.
 */
public class DocumentPathTest {

    @Test
    public void testConstruction() {
        DocumentPath path = DocumentPath.from("root.a.b");
        assertEquals(path.pathElements(), Arrays.asList("root", "a", "b"));
        assertEquals(DocumentPath.from("root.a"), path.parent());
    }

    @Test
    public void testAncestry() {
        DocumentPath path1 = DocumentPath.from("root.a.b");
        DocumentPath path2 = DocumentPath.from("root.a.d");
        DocumentPath path3 = DocumentPath.from("root.a.b.c");
        DocumentPath lca = DocumentPath.leastCommonAncestor(Arrays.asList(path1, path2, path3));
        assertEquals(DocumentPath.from("root.a"), lca);
        assertTrue(path1.isAncestorOf(path3));
        assertFalse(path1.isAncestorOf(path2));
        assertTrue(path3.isDescendentOf(path3));
        assertTrue(path3.isDescendentOf(path1));
        assertFalse(path3.isDescendentOf(path2));
    }
}
