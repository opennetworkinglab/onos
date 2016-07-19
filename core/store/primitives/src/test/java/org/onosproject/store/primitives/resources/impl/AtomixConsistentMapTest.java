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

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import io.atomix.resource.ResourceType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.util.Tools;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.MapTransaction;
import org.onosproject.store.service.Versioned;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link AtomixConsistentMap}.
 */
public class AtomixConsistentMapTest extends AtomixTestBase {

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
        return new ResourceType(AtomixConsistentMap.class);
    }

    /**
     * Tests various basic map operations.
     */
    @Test
    public void testBasicMapOperations() throws Throwable {
        basicMapOperationTests();
    }

    /**
     * Tests various map compute* operations on different cluster sizes.
     */
    @Test
    public void testMapComputeOperations() throws Throwable {
        mapComputeOperationTests();
    }

    /**
     * Tests map event notifications.
     */
    @Test
    public void testMapListeners() throws Throwable {
        mapListenerTests();
    }

    /**
     * Tests map transaction commit.
     */
    @Test
    public void testTransactionCommit() throws Throwable {
        transactionCommitTests();
    }

    /**
     * Tests map transaction rollback.
     */
    @Test
    public void testTransactionRollback() throws Throwable {
        transactionRollbackTests();
    }

    protected void basicMapOperationTests() throws Throwable {
        final byte[] rawFooValue = Tools.getBytesUtf8("Hello foo!");
        final byte[] rawBarValue = Tools.getBytesUtf8("Hello bar!");

        AtomixConsistentMap map = createAtomixClient().getResource("testBasicMapOperationMap",
                                                                   AtomixConsistentMap.class).join();

        map.isEmpty().thenAccept(result -> {
            assertTrue(result);
        }).join();

        map.put("foo", rawFooValue).thenAccept(result -> {
            assertNull(result);
        }).join();

        map.size().thenAccept(result -> {
            assertTrue(result == 1);
        }).join();

        map.isEmpty().thenAccept(result -> {
            assertFalse(result);
        }).join();

        map.putIfAbsent("foo", "Hello foo again!".getBytes()).thenAccept(result -> {
            assertNotNull(result);
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), rawFooValue));
        }).join();

        map.putIfAbsent("bar", rawBarValue).thenAccept(result -> {
            assertNull(result);
        }).join();

        map.size().thenAccept(result -> {
            assertTrue(result == 2);
        }).join();

        map.keySet().thenAccept(result -> {
            assertTrue(result.size() == 2);
            assertTrue(result.containsAll(Sets.newHashSet("foo", "bar")));
        }).join();

        map.values().thenAccept(result -> {
            assertTrue(result.size() == 2);
            List<String> rawValues =
                    result.stream().map(v -> Tools.toStringUtf8(v.value())).collect(Collectors.toList());
            assertTrue(rawValues.contains("Hello foo!"));
            assertTrue(rawValues.contains("Hello bar!"));
        }).join();

        map.entrySet().thenAccept(result -> {
            assertTrue(result.size() == 2);
            // TODO: check entries
        }).join();

        map.get("foo").thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), rawFooValue));
        }).join();

        map.remove("foo").thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), rawFooValue));
        }).join();

        map.containsKey("foo").thenAccept(result -> {
            assertFalse(result);
        }).join();

        map.get("foo").thenAccept(result -> {
            assertNull(result);
        }).join();

        map.get("bar").thenAccept(result -> {
            assertNotNull(result);
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), rawBarValue));
        }).join();

        map.containsKey("bar").thenAccept(result -> {
            assertTrue(result);
        }).join();

        map.size().thenAccept(result -> {
            assertTrue(result == 1);
        }).join();

        map.containsValue(rawBarValue).thenAccept(result -> {
            assertTrue(result);
        }).join();

        map.containsValue(rawFooValue).thenAccept(result -> {
            assertFalse(result);
        }).join();

        map.replace("bar", "Goodbye bar!".getBytes()).thenAccept(result -> {
            assertNotNull(result);
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), rawBarValue));
        }).join();

        map.replace("foo", "Goodbye foo!".getBytes()).thenAccept(result -> {
            assertNull(result);
        }).join();

        // try replace_if_value_match for a non-existent key
        map.replace("foo", "Goodbye foo!".getBytes(), rawFooValue).thenAccept(result -> {
            assertFalse(result);
        }).join();

        map.replace("bar", "Goodbye bar!".getBytes(), rawBarValue).thenAccept(result -> {
            assertTrue(result);
        }).join();

        map.replace("bar", "Goodbye bar!".getBytes(), rawBarValue).thenAccept(result -> {
            assertFalse(result);
        }).join();

        Versioned<byte[]> barValue = map.get("bar").join();
        map.replace("bar", barValue.version(), "Goodbye bar!".getBytes()).thenAccept(result -> {
            assertTrue(result);
        }).join();

        map.replace("bar", barValue.version(), rawBarValue).thenAccept(result -> {
            assertFalse(result);
        }).join();

        map.clear().join();

        map.size().thenAccept(result -> {
            assertTrue(result == 0);
        }).join();
    }

    public void mapComputeOperationTests() throws Throwable {
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");
        final byte[] value3 = Tools.getBytesUtf8("value3");

        AtomixConsistentMap map = createAtomixClient().getResource("testMapComputeOperationsMap",
                                                                   AtomixConsistentMap.class).join();

        map.computeIfAbsent("foo", k -> value1).thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), value1));
        }).join();

        map.computeIfAbsent("foo", k -> value2).thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), value1));
        }).join();

        map.computeIfPresent("bar", (k, v) -> value2).thenAccept(result -> {
            assertNull(result);
        });

        map.computeIfPresent("foo", (k, v) -> value3).thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), value3));
        }).join();

        map.computeIfPresent("foo", (k, v) -> null).thenAccept(result -> {
            assertNull(result);
        }).join();

        map.computeIf("foo", v -> v == null, (k, v) -> value1).thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), value1));
        }).join();

        map.compute("foo", (k, v) -> value2).thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), value2));
        }).join();
    }


    protected void mapListenerTests() throws Throwable {
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");
        final byte[] value3 = Tools.getBytesUtf8("value3");

        AtomixConsistentMap map = createAtomixClient().getResource("testMapListenerMap",
                                                                   AtomixConsistentMap.class).join();
        TestMapEventListener listener = new TestMapEventListener();

        // add listener; insert new value into map and verify an INSERT event is received.
        map.addListener(listener).thenCompose(v -> map.put("foo", value1)).join();
        MapEvent<String, byte[]> event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.INSERT, event.type());
        assertTrue(Arrays.equals(value1, event.newValue().value()));

        // remove listener and verify listener is not notified.
        map.removeListener(listener).thenCompose(v -> map.put("foo", value2)).join();
        assertFalse(listener.eventReceived());

        // add the listener back and verify UPDATE events are received correctly
        map.addListener(listener).thenCompose(v -> map.put("foo", value3)).join();
        event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.UPDATE, event.type());
        assertTrue(Arrays.equals(value3, event.newValue().value()));

        // perform a non-state changing operation and verify no events are received.
        map.putIfAbsent("foo", value1).join();
        assertFalse(listener.eventReceived());

        // verify REMOVE events are received correctly.
        map.remove("foo").join();
        event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.REMOVE, event.type());
        assertTrue(Arrays.equals(value3, event.oldValue().value()));

        // verify compute methods also generate events.
        map.computeIf("foo", v -> v == null, (k, v) -> value1).join();
        event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.INSERT, event.type());
        assertTrue(Arrays.equals(value1, event.newValue().value()));

        map.compute("foo", (k, v) -> value2).join();
        event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.UPDATE, event.type());
        assertTrue(Arrays.equals(value2, event.newValue().value()));

        map.computeIf("foo", v -> Arrays.equals(v, value2), (k, v) -> null).join();
        event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.REMOVE, event.type());
        assertTrue(Arrays.equals(value2, event.oldValue().value()));

        map.removeListener(listener).join();
    }

    protected void transactionCommitTests() throws Throwable {
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");

        AtomixConsistentMap map = createAtomixClient().getResource("testCommitTestsMap",
                                                                   AtomixConsistentMap.class).join();
        TestMapEventListener listener = new TestMapEventListener();

        map.addListener(listener).join();

        // PUT_IF_ABSENT
        MapUpdate<String, byte[]> update1 =
                MapUpdate.<String, byte[]>newBuilder().withType(MapUpdate.Type.PUT_IF_ABSENT)
                .withKey("foo")
                .withValue(value1)
                .build();

        MapTransaction<String, byte[]> tx = new MapTransaction<>(TransactionId.from("tx1"), Arrays.asList(update1));

        map.prepare(tx).thenAccept(result -> {
            assertEquals(true, result);
        }).join();
        // verify changes in Tx is not visible yet until commit
        assertFalse(listener.eventReceived());

        map.size().thenAccept(result -> {
            assertTrue(result == 0);
        }).join();

        map.get("foo").thenAccept(result -> {
            assertNull(result);
        }).join();

        try {
            map.put("foo", value2).join();
            fail("update to map entry in open tx should fail with Exception");
        } catch (CompletionException e) {
            assertEquals(ConcurrentModificationException.class, e.getCause().getClass());
        }

        assertFalse(listener.eventReceived());

        map.commit(tx.transactionId()).join();
        MapEvent<String, byte[]> event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.INSERT, event.type());
        assertTrue(Arrays.equals(value1, event.newValue().value()));

        // map should be update-able after commit
        map.put("foo", value2).thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), value1));
        }).join();
        event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.UPDATE, event.type());
        assertTrue(Arrays.equals(value2, event.newValue().value()));


        // REMOVE_IF_VERSION_MATCH
        byte[] currFoo = map.get("foo").get().value();
        long currFooVersion = map.get("foo").get().version();
        MapUpdate<String, byte[]> remove1 =
                MapUpdate.<String, byte[]>newBuilder().withType(MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                .withKey("foo")
                .withCurrentVersion(currFooVersion)
                .build();

        tx = new MapTransaction<>(TransactionId.from("tx2"), Arrays.asList(remove1));

        map.prepare(tx).thenAccept(result -> {
            assertTrue("prepare should succeed", result);
        }).join();
        // verify changes in Tx is not visible yet until commit
        assertFalse(listener.eventReceived());

        map.size().thenAccept(size -> {
            assertThat(size, is(1));
        }).join();

        map.get("foo").thenAccept(result -> {
            assertThat(result.value(), is(currFoo));
        }).join();

        map.commit(tx.transactionId()).join();
        event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.REMOVE, event.type());
        assertArrayEquals(currFoo, event.oldValue().value());

        map.size().thenAccept(size -> {
            assertThat(size, is(0));
        }).join();

    }

    protected void transactionRollbackTests() throws Throwable {
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");

        AtomixConsistentMap map = createAtomixClient().getResource("testTransactionRollbackTestsMap",
                                                                   AtomixConsistentMap.class).join();
        TestMapEventListener listener = new TestMapEventListener();

        map.addListener(listener).join();

        MapUpdate<String, byte[]> update1 =
                MapUpdate.<String, byte[]>newBuilder().withType(MapUpdate.Type.PUT_IF_ABSENT)
                .withKey("foo")
                .withValue(value1)
                .build();
        MapTransaction<String, byte[]> tx = new MapTransaction<>(TransactionId.from("tx1"), Arrays.asList(update1));
        map.prepare(tx).thenAccept(result -> {
            assertEquals(true, result);
        }).join();
        assertFalse(listener.eventReceived());

        map.rollback(tx.transactionId()).join();
        assertFalse(listener.eventReceived());

        map.get("foo").thenAccept(result -> {
            assertNull(result);
        }).join();

        map.put("foo", value2).thenAccept(result -> {
            assertNull(result);
        }).join();
        MapEvent<String, byte[]> event = listener.event();
        assertNotNull(event);
        assertEquals(MapEvent.Type.INSERT, event.type());
        assertTrue(Arrays.equals(value2, event.newValue().value()));
    }

    private static class TestMapEventListener implements MapEventListener<String, byte[]> {

        private final BlockingQueue<MapEvent<String, byte[]>> queue = new ArrayBlockingQueue<>(1);

        @Override
        public void event(MapEvent<String, byte[]> event) {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                Throwables.propagate(e);
            }
        }

        public boolean eventReceived() {
            return !queue.isEmpty();
        }

        public MapEvent<String, byte[]> event() throws InterruptedException {
            return queue.take();
        }
    }
}
