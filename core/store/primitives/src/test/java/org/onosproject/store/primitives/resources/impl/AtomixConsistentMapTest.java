/*
 * Copyright 2016 Open Networking Laboratory
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

import io.atomix.resource.ResourceType;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.onlab.util.Tools;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.Sets;

/**
 * Unit tests for {@link AtomixConsistentMap}.
 */
@Ignore
public class AtomixConsistentMapTest extends AtomixTestBase {

    @Override
    protected ResourceType resourceType() {
        return new ResourceType(AtomixConsistentMap.class);
    }

    /**
     * Tests various basic map operations.
     */
    @Test
    public void testBasicMapOperations() throws Throwable {
        basicMapOperationTests(1);
        clearTests();
        basicMapOperationTests(2);
        clearTests();
        basicMapOperationTests(3);
    }

    /**
     * Tests various map compute* operations on different cluster sizes.
     */
    @Test
    public void testMapComputeOperations() throws Throwable {
        mapComputeOperationTests(1);
        clearTests();
        mapComputeOperationTests(2);
        clearTests();
        mapComputeOperationTests(3);
    }

    /**
     * Tests map event notifications.
     */
    @Test
    public void testMapListeners() throws Throwable {
        mapListenerTests(1);
        clearTests();
        mapListenerTests(2);
        clearTests();
        mapListenerTests(3);
    }

    /**
     * Tests map transaction commit.
     */
    @Test
    public void testTransactionCommit() throws Throwable {
        transactionCommitTests(1);
        clearTests();
        transactionCommitTests(2);
        clearTests();
        transactionCommitTests(3);
    }

    /**
     * Tests map transaction rollback.
     */
    @Test
    public void testTransactionRollback() throws Throwable {
        transactionRollbackTests(1);
        clearTests();
        transactionRollbackTests(2);
        clearTests();
        transactionRollbackTests(3);
    }

    protected void basicMapOperationTests(int clusterSize) throws Throwable {
        createCopycatServers(clusterSize);

        final byte[] rawFooValue = Tools.getBytesUtf8("Hello foo!");
        final byte[] rawBarValue = Tools.getBytesUtf8("Hello bar!");

        AtomixConsistentMap map = createAtomixClient().get("test", AtomixConsistentMap.class).join();

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

    public void mapComputeOperationTests(int clusterSize) throws Throwable {
        createCopycatServers(clusterSize);
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");
        final byte[] value3 = Tools.getBytesUtf8("value3");

        AtomixConsistentMap map = createAtomixClient().get("test", AtomixConsistentMap.class).join();

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


    protected void mapListenerTests(int clusterSize) throws Throwable {
        createCopycatServers(clusterSize);
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");
        final byte[] value3 = Tools.getBytesUtf8("value3");

        AtomixConsistentMap map = createAtomixClient().get("test", AtomixConsistentMap.class).join();
        TestMapEventListener listener = new TestMapEventListener();

        // add listener; insert new value into map and verify an INSERT event is received.
        map.addListener(listener).join();
        map.put("foo", value1).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.INSERT, listener.event().type());
        assertTrue(Arrays.equals(value1, listener.event().newValue().value()));
        listener.clearEvent();

        // remove listener and verify listener is not notified.
        map.removeListener(listener).join();
        map.put("foo", value2).join();
        assertNull(listener.event());

        // add the listener back and verify UPDATE events are received correctly
        map.addListener(listener).join();
        map.put("foo", value3).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.UPDATE, listener.event().type());
        assertTrue(Arrays.equals(value3, listener.event().newValue().value()));
        listener.clearEvent();

        // perform a non-state changing operation and verify no events are received.
        map.putIfAbsent("foo", value1).join();
        assertNull(listener.event());

        // verify REMOVE events are received correctly.
        map.remove("foo").join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.REMOVE, listener.event().type());
        assertTrue(Arrays.equals(value3, listener.event().oldValue().value()));
        listener.clearEvent();

        // verify compute methods also generate events.
        map.computeIf("foo", v -> v == null, (k, v) -> value1).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.INSERT, listener.event().type());
        assertTrue(Arrays.equals(value1, listener.event().newValue().value()));
        listener.clearEvent();

        map.compute("foo", (k, v) -> value2).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.UPDATE, listener.event().type());
        assertTrue(Arrays.equals(value2, listener.event().newValue().value()));
        listener.clearEvent();

        map.computeIf("foo", v -> Arrays.equals(v, value2), (k, v) -> null).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.REMOVE, listener.event().type());
        assertTrue(Arrays.equals(value2, listener.event().oldValue().value()));
        listener.clearEvent();

        map.removeListener(listener).join();
    }

    protected void transactionCommitTests(int clusterSize) throws Throwable {
        createCopycatServers(clusterSize);
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");

        AtomixConsistentMap map = createAtomixClient().get("test", AtomixConsistentMap.class).join();
        TestMapEventListener listener = new TestMapEventListener();

        map.addListener(listener).join();

        MapUpdate<String, byte[]> update1 =
                MapUpdate.<String, byte[]>newBuilder().withType(MapUpdate.Type.PUT_IF_ABSENT)
                .withKey("foo")
                .withValue(value1)
                .build();

        TransactionalMapUpdate<String, byte[]> txMapUpdate =
                new TransactionalMapUpdate<>(TransactionId.from("tx1"), Arrays.asList(update1));

        map.prepare(txMapUpdate).thenAccept(result -> {
            assertEquals(PrepareResult.OK, result);
        }).join();
        assertNull(listener.event());

        map.size().thenAccept(result -> {
            assertTrue(result == 0);
        }).join();

        map.get("foo").thenAccept(result -> {
            assertNull(result);
        }).join();

        try {
            map.put("foo", value2).join();
            assertTrue(false);
        } catch (CompletionException e) {
            assertEquals(ConcurrentModificationException.class, e.getCause().getClass());
        }

        assertNull(listener.event());

        map.commit(txMapUpdate.transactionId()).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.INSERT, listener.event().type());
        assertTrue(Arrays.equals(value1, listener.event().newValue().value()));
        listener.clearEvent();

        map.put("foo", value2).thenAccept(result -> {
            assertTrue(Arrays.equals(Versioned.valueOrElse(result, null), value1));
        }).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.UPDATE, listener.event().type());
        assertTrue(Arrays.equals(value2, listener.event().newValue().value()));
        listener.clearEvent();
    }

    protected void transactionRollbackTests(int clusterSize) throws Throwable {
        createCopycatServers(clusterSize);
        final byte[] value1 = Tools.getBytesUtf8("value1");
        final byte[] value2 = Tools.getBytesUtf8("value2");

        AtomixConsistentMap map = createAtomixClient().get("test", AtomixConsistentMap.class).join();
        TestMapEventListener listener = new TestMapEventListener();

        map.addListener(listener).join();

        MapUpdate<String, byte[]> update1 =
                MapUpdate.<String, byte[]>newBuilder().withType(MapUpdate.Type.PUT_IF_ABSENT)
                .withKey("foo")
                .withValue(value1)
                .build();
        TransactionalMapUpdate<String, byte[]> txMapUpdate =
                new TransactionalMapUpdate<>(TransactionId.from("tx1"), Arrays.asList(update1));
        map.prepare(txMapUpdate).thenAccept(result -> {
            assertEquals(PrepareResult.OK, result);
        }).join();
        assertNull(listener.event());

        map.rollback(txMapUpdate.transactionId()).join();
        assertNull(listener.event());

        map.get("foo").thenAccept(result -> {
            assertNull(result);
        }).join();

        map.put("foo", value2).thenAccept(result -> {
            assertNull(result);
        }).join();
        assertNotNull(listener.event());
        assertEquals(MapEvent.Type.INSERT, listener.event().type());
        assertTrue(Arrays.equals(value2, listener.event().newValue().value()));
        listener.clearEvent();
    }

    private static class TestMapEventListener implements MapEventListener<String, byte[]> {

        MapEvent<String, byte[]> event;

        @Override
        public void event(MapEvent<String, byte[]> event) {
            this.event = event;
        }

        public MapEvent<String, byte[]> event() {
            return event;
        }

        public void clearEvent() {
            event = null;
        }
    }
}
