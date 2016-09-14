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

package org.onosproject.store.primitives.resources.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.atomix.resource.ResourceType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.Versioned;

import com.google.common.base.Throwables;

/**
 * Unit tests for {@link AtomixDocumentTree}.
 */
public class AtomixDocumentTreeTest extends AtomixTestBase {
    @BeforeClass
    public static void preTestSetup() throws Throwable {
        createCopycatServers(3);
    }

    @AfterClass
    public static void postTestCleanup() throws Exception {
        clearTests();
    }
    @Override
    protected ResourceType resourceType() {
        return new ResourceType(AtomixDocumentTree.class);
    }
    /**
     * Tests queries (get and getChildren).
     */
    @Test
    public void testQueries() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        Versioned<byte[]> root = tree.get(DocumentPath.from("root")).join();
        assertEquals(1, root.version());
        assertNull(root.value());
    }

    /**
     * Tests create.
     */
    @Test
    public void testCreate() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();
        Versioned<byte[]> a = tree.get(DocumentPath.from("root.a")).join();
        assertArrayEquals("a".getBytes(), a.value());

        Versioned<byte[]> ab = tree.get(DocumentPath.from("root.a.b")).join();
        assertArrayEquals("ab".getBytes(), ab.value());

        Versioned<byte[]> ac = tree.get(DocumentPath.from("root.a.c")).join();
        assertArrayEquals("ac".getBytes(), ac.value());
    }

    /**
     * Tests set.
     */
    @Test
    public void testSet() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();

        tree.set(DocumentPath.from("root.a.d"), "ad".getBytes()).join();
        Versioned<byte[]> ad = tree.get(DocumentPath.from("root.a.d")).join();
        assertArrayEquals("ad".getBytes(), ad.value());

        tree.set(DocumentPath.from("root.a"), "newA".getBytes()).join();
        Versioned<byte[]> newA = tree.get(DocumentPath.from("root.a")).join();
        assertArrayEquals("newA".getBytes(), newA.value());

        tree.set(DocumentPath.from("root.a.b"), "newAB".getBytes()).join();
        Versioned<byte[]> newAB = tree.get(DocumentPath.from("root.a.b")).join();
        assertArrayEquals("newAB".getBytes(), newAB.value());
    }

    /**
     * Tests replace if version matches.
     */
    @Test
    public void testReplaceVersion() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();

        Versioned<byte[]> ab = tree.get(DocumentPath.from("root.a.b")).join();
        assertTrue(tree.replace(DocumentPath.from("root.a.b"), "newAB".getBytes(), ab.version()).join());
        Versioned<byte[]> newAB = tree.get(DocumentPath.from("root.a.b")).join();
        assertArrayEquals("newAB".getBytes(), newAB.value());

        assertFalse(tree.replace(DocumentPath.from("root.a.b"), "newestAB".getBytes(), ab.version()).join());
        assertArrayEquals("newAB".getBytes(), tree.get(DocumentPath.from("root.a.b")).join().value());

        assertFalse(tree.replace(DocumentPath.from("root.a.d"), "foo".getBytes(), 1).join());
    }

    /**
     * Tests replace if value matches.
     */
    @Test
    public void testReplaceValue() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();

        Versioned<byte[]> ab = tree.get(DocumentPath.from("root.a.b")).join();
        assertTrue(tree.replace(DocumentPath.from("root.a.b"), "newAB".getBytes(), ab.value()).join());
        Versioned<byte[]> newAB = tree.get(DocumentPath.from("root.a.b")).join();
        assertArrayEquals("newAB".getBytes(), newAB.value());

        assertFalse(tree.replace(DocumentPath.from("root.a.b"), "newestAB".getBytes(), ab.value()).join());
        assertArrayEquals("newAB".getBytes(), tree.get(DocumentPath.from("root.a.b")).join().value());

        assertFalse(tree.replace(DocumentPath.from("root.a.d"), "bar".getBytes(), "foo".getBytes()).join());
    }

    /**
     * Tests remove.
     */
    @Test
    public void testRemove() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();

        Versioned<byte[]> ab = tree.removeNode(DocumentPath.from("root.a.b")).join();
        assertArrayEquals("ab".getBytes(), ab.value());
        assertNull(tree.get(DocumentPath.from("root.a.b")).join());

        Versioned<byte[]> ac = tree.removeNode(DocumentPath.from("root.a.c")).join();
        assertArrayEquals("ac".getBytes(), ac.value());
        assertNull(tree.get(DocumentPath.from("root.a.c")).join());

        Versioned<byte[]> a = tree.removeNode(DocumentPath.from("root.a")).join();
        assertArrayEquals("a".getBytes(), a.value());
        assertNull(tree.get(DocumentPath.from("root.a")).join());
    }

    /**
     * Tests invalid removes.
     */
    @Test
    public void testRemoveFailures() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();

        try {
            tree.removeNode(DocumentPath.from("root")).join();
            fail();
        } catch (Exception e) {
            assertTrue(Throwables.getRootCause(e) instanceof IllegalDocumentModificationException);
        }

        try {
            tree.removeNode(DocumentPath.from("root.a")).join();
            fail();
        } catch (Exception e) {
            assertTrue(Throwables.getRootCause(e) instanceof IllegalDocumentModificationException);
        }

        try {
            tree.removeNode(DocumentPath.from("root.d")).join();
            fail();
        } catch (Exception e) {
            assertTrue(Throwables.getRootCause(e) instanceof NoSuchDocumentPathException);
        }
    }

    /**
     * Tests invalid create.
     */
    @Test
    public void testCreateFailures() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        try {
            tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();
            fail();
        } catch (Exception e) {
            assertTrue(Throwables.getRootCause(e) instanceof IllegalDocumentModificationException);
        }
    }

    /**
     * Tests invalid set.
     */
    @Test
    public void testSetFailures() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        try {
            tree.set(DocumentPath.from("root.a.c"), "ac".getBytes()).join();
            fail();
        } catch (Exception e) {
            assertTrue(Throwables.getRootCause(e) instanceof IllegalDocumentModificationException);
        }
    }

    /**
     * Tests getChildren.
     */
    @Test
    public void testGetChildren() throws Throwable {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();

        Map<String, Versioned<byte[]>> rootChildren = tree.getChildren(DocumentPath.from("root")).join();
        assertEquals(1, rootChildren.size());
        Versioned<byte[]> a = rootChildren.get("a");
        assertArrayEquals("a".getBytes(), a.value());

        Map<String, Versioned<byte[]>> children = tree.getChildren(DocumentPath.from("root.a")).join();
        assertEquals(2, children.size());
        Versioned<byte[]> ab = children.get("b");
        assertArrayEquals("ab".getBytes(), ab.value());
        Versioned<byte[]> ac = children.get("c");
        assertArrayEquals("ac".getBytes(), ac.value());

        assertEquals(0, tree.getChildren(DocumentPath.from("root.a.b")).join().size());
        assertEquals(0, tree.getChildren(DocumentPath.from("root.a.c")).join().size());
    }

    /**
     * Tests destroy.
     */
    @Test
    public void testClear() {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        tree.create(DocumentPath.from("root.a"), "a".getBytes()).join();
        tree.create(DocumentPath.from("root.a.b"), "ab".getBytes()).join();
        tree.create(DocumentPath.from("root.a.c"), "ac".getBytes()).join();

        tree.destroy().join();
        assertEquals(0, tree.getChildren(DocumentPath.from("root")).join().size());
    }

    /**
     * Tests listeners.
     */
    @Test
    @Ignore
    public void testNotifications() throws Exception {
        AtomixDocumentTree tree = createAtomixClient().getResource(UUID.randomUUID().toString(),
                AtomixDocumentTree.class).join();
        TestEventListener listener = new TestEventListener();

        // add listener; create a node in the tree and verify an CREATED event is received.
        tree.addListener(listener).thenCompose(v -> tree.set(DocumentPath.from("root.a"), "a".getBytes())).join();
        DocumentTreeEvent<byte[]> event = listener.event();
        assertNotNull(event);
        assertEquals(DocumentTreeEvent.Type.CREATED, event.type());
        assertArrayEquals("a".getBytes(), event.newValue().get().value());
    }

    private static class TestEventListener implements DocumentTreeListener<byte[]> {

        private final BlockingQueue<DocumentTreeEvent<byte[]>> queue = new ArrayBlockingQueue<>(1);

        @Override
        public void event(DocumentTreeEvent<byte[]> event) {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                Throwables.propagate(e);
            }
        }

        public DocumentTreeEvent<byte[]> event() throws InterruptedException {
            return queue.take();
        }
    }
}
