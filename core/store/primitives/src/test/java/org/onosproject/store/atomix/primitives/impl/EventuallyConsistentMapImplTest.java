/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.primitives.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static junit.framework.TestCase.assertFalse;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.AbstractEvent;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationServiceAdapter;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.persistence.TestPersistenceService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Unit tests for EventuallyConsistentMapImpl.
 */
public class EventuallyConsistentMapImplTest {

    private EventuallyConsistentMap<String, String> ecMap;

    private PersistenceService persistenceService;
    private ClusterCommunicationService clusterCommunicator;
    private SequentialClockService<String, String> clockService;

    private static final String MAP_NAME = "test";
    private static final MessageSubject BOOTSTRAP_MESSAGE_SUBJECT
            = new MessageSubject("ecm-" + MAP_NAME + "-bootstrap");
    private static final MessageSubject INITIALIZE_MESSAGE_SUBJECT
            = new MessageSubject("ecm-" + MAP_NAME + "-initialize");
    private static final MessageSubject UPDATE_MESSAGE_SUBJECT
            = new MessageSubject("ecm-" + MAP_NAME + "-update");
    private static final MessageSubject ANTI_ENTROPY_MESSAGE_SUBJECT
            = new MessageSubject("ecm-" + MAP_NAME + "-anti-entropy");
    private static final MessageSubject UPDATE_REQUEST_SUBJECT
            = new MessageSubject("ecm-" + MAP_NAME + "-update-request");

    private static final String KEY1 = "one";
    private static final String KEY2 = "two";
    private static final String VALUE1 = "oneValue";
    private static final String VALUE2 = "twoValue";

    private final ControllerNode self =
            new DefaultControllerNode(new NodeId("local"), IpAddress.valueOf(1));

    private Consumer<Collection<UpdateEntry<String, String>>> updateHandler;
    private Consumer<Collection<UpdateRequest<String>>> requestHandler;
    private Function<AntiEntropyAdvertisement<String>, AntiEntropyResponse> antiEntropyHandler;
    private Supplier<List<NodeId>> peersHandler = ArrayList::new;

    @Before
    public void setUp() throws Exception {
        clusterCommunicator = createMock(ClusterCommunicationService.class);

        persistenceService = new TestPersistenceService();
        // Add expectation for adding cluster message subscribers which
        // delegate to our ClusterCommunicationService implementation. This
        // allows us to get a reference to the map's internal cluster message
        // handlers so we can induce events coming in from a peer.
        clusterCommunicator.<Object, Object>addSubscriber(anyObject(MessageSubject.class),
                anyObject(Function.class),
                anyObject(Function.class),
                anyObject(Function.class));
        expectLastCall().andDelegateTo(new TestClusterCommunicationService()).times(1);
        clusterCommunicator.<Object, Object>addSubscriber(anyObject(MessageSubject.class),
                anyObject(Function.class),
                anyObject(Function.class),
                anyObject(Function.class),
                anyObject(Executor.class));
        expectLastCall().andDelegateTo(new TestClusterCommunicationService()).times(1);
        clusterCommunicator.<Object>addSubscriber(anyObject(MessageSubject.class),
                anyObject(Function.class), anyObject(Consumer.class), anyObject(Executor.class));
        expectLastCall().andDelegateTo(new TestClusterCommunicationService()).times(1);
        clusterCommunicator.<Object, Object>addSubscriber(anyObject(MessageSubject.class),
                                                          anyObject(Function.class),
                                                          anyObject(Function.class),
                                                          anyObject(Function.class),
                                                          anyObject(Executor.class));
        expectLastCall().andDelegateTo(new TestClusterCommunicationService()).times(1);
        clusterCommunicator.<Object>addSubscriber(anyObject(MessageSubject.class),
                anyObject(Function.class), anyObject(Consumer.class), anyObject(Executor.class));
        expectLastCall().andDelegateTo(new TestClusterCommunicationService()).times(1);

        replay(clusterCommunicator);

        clockService = new SequentialClockService<>();

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(TestTimestamp.class);

        ecMap = new EventuallyConsistentMapBuilderImpl<String, String>(
                NodeId.nodeId("0"),
                clusterCommunicator,
                persistenceService,
                peersHandler,
                peersHandler
                )
                .withName(MAP_NAME)
                .withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp(k, v))
                .withCommunicationExecutor(MoreExecutors.newDirectExecutorService())
                .withPersistence()
                .build();

        // Reset ready for tests to add their own expectations
        reset(clusterCommunicator);
    }

    @After
    public void tearDown() {
        reset(clusterCommunicator);
        ecMap.destroy();
    }

    @SuppressWarnings("unchecked")
    private EventuallyConsistentMapListener<String, String> getListener() {
        return createMock(EventuallyConsistentMapListener.class);
    }

    @Test
    public void testSize() throws Exception {
        expectPeerMessage(clusterCommunicator);

        assertEquals(0, ecMap.size());
        ecMap.put(KEY1, VALUE1);
        assertEquals(1, ecMap.size());
        ecMap.put(KEY1, VALUE2);
        assertEquals(1, ecMap.size());
        ecMap.put(KEY2, VALUE2);
        assertEquals(2, ecMap.size());
        for (int i = 0; i < 10; i++) {
            ecMap.put("" + i, "" + i);
        }
        assertEquals(12, ecMap.size());
        ecMap.remove(KEY1);
        assertEquals(11, ecMap.size());
        ecMap.remove(KEY1);
        assertEquals(11, ecMap.size());
    }

    @Test
    public void testIsEmpty() throws Exception {
        expectPeerMessage(clusterCommunicator);

        assertTrue(ecMap.isEmpty());
        ecMap.put(KEY1, VALUE1);
        assertFalse(ecMap.isEmpty());
        ecMap.remove(KEY1);
        assertTrue(ecMap.isEmpty());
    }

    @Test
    public void testContainsKey() throws Exception {
        expectPeerMessage(clusterCommunicator);

        assertFalse(ecMap.containsKey(KEY1));
        ecMap.put(KEY1, VALUE1);
        assertTrue(ecMap.containsKey(KEY1));
        assertFalse(ecMap.containsKey(KEY2));
        ecMap.remove(KEY1);
        assertFalse(ecMap.containsKey(KEY1));
    }

    @Test
    public void testContainsValue() throws Exception {
        expectPeerMessage(clusterCommunicator);

        assertFalse(ecMap.containsValue(VALUE1));
        ecMap.put(KEY1, VALUE1);
        assertTrue(ecMap.containsValue(VALUE1));
        assertFalse(ecMap.containsValue(VALUE2));
        ecMap.put(KEY1, VALUE2);
        assertFalse(ecMap.containsValue(VALUE1));
        assertTrue(ecMap.containsValue(VALUE2));
        ecMap.remove(KEY1);
        assertFalse(ecMap.containsValue(VALUE2));
    }

    @Test
    public void testGet() throws Exception {
        expectPeerMessage(clusterCommunicator);

        CountDownLatch latch;

        // Local put
        assertNull(ecMap.get(KEY1));
        ecMap.put(KEY1, VALUE1);
        assertEquals(VALUE1, ecMap.get(KEY1));

        // Remote put
        List<UpdateEntry<String, String>> message
                = ImmutableList.of(generatePutMessage(KEY2, VALUE2, clockService.getTimestamp(KEY2, VALUE2)));

        // Create a latch so we know when the put operation has finished
        latch = new CountDownLatch(1);
        ecMap.addListener(new TestListener(latch));

        assertNull(ecMap.get(KEY2));
        updateHandler.accept(message);
        assertTrue("External listener never got notified of internal event",
                   latch.await(100, TimeUnit.MILLISECONDS));
        assertEquals(VALUE2, ecMap.get(KEY2));

        // Local remove
        ecMap.remove(KEY2);
        assertNull(ecMap.get(KEY2));

        // Remote remove
        message = ImmutableList.of(generateRemoveMessage(KEY1, clockService.getTimestamp(KEY1, VALUE1)));

        // Create a latch so we know when the remove operation has finished
        latch = new CountDownLatch(1);
        ecMap.addListener(new TestListener(latch));

        updateHandler.accept(message);
        assertTrue("External listener never got notified of internal event",
                   latch.await(100, TimeUnit.MILLISECONDS));
        assertNull(ecMap.get(KEY1));
    }

    @Test
    public void testPut() throws Exception {
        // Set up expectations of external events to be sent to listeners during
        // the test. These don't use timestamps so we can set them all up at once.
        EventuallyConsistentMapListener<String, String> listener
                = getListener();
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY1, VALUE2));
        replay(listener);

        ecMap.addListener(listener);

        // Set up expected internal message to be broadcast to peers on first put
        expectSpecificMulticastMessage(generatePutMessage(KEY1, VALUE1, clockService
                .peekAtNextTimestamp()), UPDATE_MESSAGE_SUBJECT, clusterCommunicator);

        // Put first value
        assertNull(ecMap.get(KEY1));
        ecMap.put(KEY1, VALUE1);
        assertEquals(VALUE1, ecMap.get(KEY1));

        verify(clusterCommunicator);

        // Set up expected internal message to be broadcast to peers on second put
        expectSpecificMulticastMessage(generatePutMessage(
                KEY1, VALUE2, clockService.peekAtNextTimestamp()), UPDATE_MESSAGE_SUBJECT, clusterCommunicator);

        // Update same key to a new value
        ecMap.put(KEY1, VALUE2);
        assertEquals(VALUE2, ecMap.get(KEY1));

        verify(clusterCommunicator);

        // Do a put with a older timestamp than the value already there.
        // The map data should not be changed and no notifications should be sent.
        reset(clusterCommunicator);
        replay(clusterCommunicator);

        clockService.turnBackTime();
        ecMap.put(KEY1, VALUE1);
        // Value should not have changed.
        assertEquals(VALUE2, ecMap.get(KEY1));

        verify(clusterCommunicator);

        // Check that our listener received the correct events during the test
        verify(listener);
    }

    @Test
    public void testRemove() throws Exception {
        // Set up expectations of external events to be sent to listeners during
        // the test. These don't use timestamps so we can set them all up at once.
        EventuallyConsistentMapListener<String, String> listener
                = getListener();
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.REMOVE, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY2, VALUE2));
        replay(listener);

        ecMap.addListener(listener);

        // Put in an initial value
        expectPeerMessage(clusterCommunicator);
        ecMap.put(KEY1, VALUE1);
        assertEquals(VALUE1, ecMap.get(KEY1));

        // Remove the value and check the correct internal cluster messages
        // are sent
        expectSpecificMulticastMessage(generateRemoveMessage(KEY1, clockService.peekAtNextTimestamp()),
                UPDATE_MESSAGE_SUBJECT, clusterCommunicator);

        ecMap.remove(KEY1);
        assertNull(ecMap.get(KEY1));

        verify(clusterCommunicator);

        // Remove the same value again. Even though the value is no longer in
        // the map, we expect that the tombstone is updated and another remove
        // event is sent to the cluster and external listeners.
        expectSpecificMulticastMessage(generateRemoveMessage(KEY1, clockService.peekAtNextTimestamp()),
                UPDATE_MESSAGE_SUBJECT, clusterCommunicator);

        ecMap.remove(KEY1);
        assertNull(ecMap.get(KEY1));

        verify(clusterCommunicator);


        // Put in a new value for us to try and remove
        expectPeerMessage(clusterCommunicator);

        ecMap.put(KEY2, VALUE2);

        clockService.turnBackTime();

        // Remove should have no effect, since it has an older timestamp than
        // the put. Expect no notifications to be sent out
        reset(clusterCommunicator);
        replay(clusterCommunicator);

        ecMap.remove(KEY2);

        verify(clusterCommunicator);

        // Check that our listener received the correct events during the test
        verify(listener);
    }

    @Test
    public void testCompute() throws Exception {
        // Set up expectations of external events to be sent to listeners during
        // the test. These don't use timestamps so we can set them all up at once.
        EventuallyConsistentMapListener<String, String> listener
                = getListener();
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.REMOVE, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY2, VALUE2));
        replay(listener);

        ecMap.addListener(listener);

        // Put in an initial value
        expectPeerMessage(clusterCommunicator);
        ecMap.compute(KEY1, (k, v) -> VALUE1);
        assertEquals(VALUE1, ecMap.get(KEY1));

        // Remove the value and check the correct internal cluster messages
        // are sent
        expectSpecificMulticastMessage(generateRemoveMessage(KEY1, clockService.peekAtNextTimestamp()),
                UPDATE_MESSAGE_SUBJECT, clusterCommunicator);

        ecMap.compute(KEY1, (k, v) -> null);
        assertNull(ecMap.get(KEY1));

        verify(clusterCommunicator);

        // Remove the same value again. Even though the value is no longer in
        // the map, we expect that the tombstone is updated and another remove
        // event is sent to the cluster and external listeners.
        expectSpecificMulticastMessage(generateRemoveMessage(KEY1, clockService.peekAtNextTimestamp()),
                UPDATE_MESSAGE_SUBJECT, clusterCommunicator);

        ecMap.compute(KEY1, (k, v) -> null);
        assertNull(ecMap.get(KEY1));

        verify(clusterCommunicator);

        // Put in a new value for us to try and remove
        expectPeerMessage(clusterCommunicator);

        ecMap.compute(KEY2, (k, v) -> VALUE2);

        clockService.turnBackTime();

        // Remove should have no effect, since it has an older timestamp than
        // the put. Expect no notifications to be sent out
        reset(clusterCommunicator);
        replay(clusterCommunicator);

        ecMap.compute(KEY2, (k, v) -> null);

        verify(clusterCommunicator);

        // Check that our listener received the correct events during the test
        verify(listener);
    }

    @Test
    public void testPutAll() throws Exception {
        // putAll() with an empty map is a no-op - no messages will be sent
        reset(clusterCommunicator);
        replay(clusterCommunicator);

        ecMap.putAll(new HashMap<>());

        verify(clusterCommunicator);

        // Set up the listener with our expected events
        EventuallyConsistentMapListener<String, String> listener
                = getListener();
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY2, VALUE2));
        replay(listener);

        ecMap.addListener(listener);

        // Expect a multi-update inter-instance message
        expectSpecificBroadcastMessage(generatePutMessage(KEY1, VALUE1, KEY2, VALUE2), UPDATE_MESSAGE_SUBJECT,
                                       clusterCommunicator);

        Map<String, String> putAllValues = new HashMap<>();
        putAllValues.put(KEY1, VALUE1);
        putAllValues.put(KEY2, VALUE2);

        // Put the values in the map
        ecMap.putAll(putAllValues);

        // Check the correct messages and events were sent
        verify(clusterCommunicator);
        verify(listener);
    }

    @Test
    public void testClear() throws Exception {
        EventuallyConsistentMapListener<String, String> listener
                = getListener();
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.PUT, KEY2, VALUE2));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.REMOVE, KEY1, VALUE1));
        listener.event(new EventuallyConsistentMapEvent<>(
                MAP_NAME, EventuallyConsistentMapEvent.Type.REMOVE, KEY2, VALUE2));
        replay(listener);

        // clear() on an empty map is a no-op - no messages will be sent
        reset(clusterCommunicator);
        replay(clusterCommunicator);

        assertTrue(ecMap.isEmpty());
        ecMap.clear();
        verify(clusterCommunicator);

        // Put some items in the map
        expectPeerMessage(clusterCommunicator);
        ecMap.put(KEY1, VALUE1);
        ecMap.put(KEY2, VALUE2);

        ecMap.addListener(listener);
        expectSpecificBroadcastMessage(generateRemoveMessage(KEY1, KEY2), UPDATE_MESSAGE_SUBJECT, clusterCommunicator);

        ecMap.clear();

        verify(clusterCommunicator);
        verify(listener);
    }

    @Test
    public void testKeySet() throws Exception {
        expectPeerMessage(clusterCommunicator);

        assertTrue(ecMap.keySet().isEmpty());

        // Generate some keys
        Set<String> keys = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            keys.add("" + i);
        }

        // Put each key in the map
        keys.forEach(k -> ecMap.put(k, "value" + k));

        // Check keySet() returns the correct value
        assertEquals(keys, ecMap.keySet());

        // Update the value for one of the keys
        ecMap.put(keys.iterator().next(), "new-value");

        // Check the key set is still the same
        assertEquals(keys, ecMap.keySet());

        // Remove a key
        String removeKey = keys.iterator().next();
        keys.remove(removeKey);
        ecMap.remove(removeKey);

        // Check the key set is still correct
        assertEquals(keys, ecMap.keySet());
    }

    @Test
    public void testValues() throws Exception {
        expectPeerMessage(clusterCommunicator);

        assertTrue(ecMap.values().isEmpty());

        // Generate some values
        Map<String, String> expectedValues = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            expectedValues.put("" + i, "value" + i);
        }

        // Add them into the map
        expectedValues.entrySet().forEach(e -> ecMap.put(e.getKey(), e.getValue()));

        // Check the values collection is correct
        assertEquals(expectedValues.values().size(), ecMap.values().size());
        expectedValues.values().forEach(v -> assertTrue(ecMap.values().contains(v)));

        // Update the value for one of the keys
        Map.Entry<String, String> first = expectedValues.entrySet().iterator().next();
        expectedValues.put(first.getKey(), "new-value");
        ecMap.put(first.getKey(), "new-value");

        // Check the values collection is still correct
        assertEquals(expectedValues.values().size(), ecMap.values().size());
        expectedValues.values().forEach(v -> assertTrue(ecMap.values().contains(v)));

        // Remove a key
        String removeKey = expectedValues.keySet().iterator().next();
        expectedValues.remove(removeKey);
        ecMap.remove(removeKey);

        // Check the values collection is still correct
        assertEquals(expectedValues.values().size(), ecMap.values().size());
        expectedValues.values().forEach(v -> assertTrue(ecMap.values().contains(v)));
    }

    @Test
    public void testEntrySet() throws Exception {
        expectPeerMessage(clusterCommunicator);

        assertTrue(ecMap.entrySet().isEmpty());

        // Generate some values
        Map<String, String> expectedValues = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            expectedValues.put("" + i, "value" + i);
        }

        // Add them into the map
        expectedValues.entrySet().forEach(e -> ecMap.put(e.getKey(), e.getValue()));

        // Check the entry set is correct
        assertTrue(entrySetsAreEqual(expectedValues, ecMap.entrySet()));

        // Update the value for one of the keys
        Map.Entry<String, String> first = expectedValues.entrySet().iterator().next();
        expectedValues.put(first.getKey(), "new-value");
        ecMap.put(first.getKey(), "new-value");

        // Check the entry set is still correct
        assertTrue(entrySetsAreEqual(expectedValues, ecMap.entrySet()));

        // Remove a key
        String removeKey = expectedValues.keySet().iterator().next();
        expectedValues.remove(removeKey);
        ecMap.remove(removeKey);

        // Check the entry set is still correct
        assertTrue(entrySetsAreEqual(expectedValues, ecMap.entrySet()));
    }

    private static boolean entrySetsAreEqual(Map<String, String> expectedMap, Set<Map.Entry<String, String>> actual) {
        if (expectedMap.entrySet().size() != actual.size()) {
            return false;
        }

        for (Map.Entry<String, String> e : actual) {
            if (!expectedMap.containsKey(e.getKey())) {
                return false;
            }
            if (!Objects.equals(expectedMap.get(e.getKey()), e.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testDestroy() throws Exception {
        clusterCommunicator.removeSubscriber(BOOTSTRAP_MESSAGE_SUBJECT);
        clusterCommunicator.removeSubscriber(INITIALIZE_MESSAGE_SUBJECT);
        clusterCommunicator.removeSubscriber(UPDATE_MESSAGE_SUBJECT);
        clusterCommunicator.removeSubscriber(UPDATE_REQUEST_SUBJECT);
        clusterCommunicator.removeSubscriber(ANTI_ENTROPY_MESSAGE_SUBJECT);

        replay(clusterCommunicator);

        ecMap.destroy();

        verify(clusterCommunicator);

        try {
            ecMap.get(KEY1);
            fail("get after destroy should throw exception");
        } catch (IllegalStateException e) {
            assertTrue(true);
        }

        try {
            ecMap.put(KEY1, VALUE1);
            fail("put after destroy should throw exception");
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    private UpdateEntry<String, String> generatePutMessage(String key, String value, Timestamp timestamp) {
        return new UpdateEntry<>(key, new MapValue<>(value, timestamp));
    }

    private List<UpdateEntry<String, String>> generatePutMessage(
            String key1, String value1, String key2, String value2) {
        List<UpdateEntry<String, String>> list = new ArrayList<>();

        Timestamp timestamp1 = clockService.peek(1);
        Timestamp timestamp2 = clockService.peek(2);

        list.add(generatePutMessage(key1, value1, timestamp1));
        list.add(generatePutMessage(key2, value2, timestamp2));

        return list;
    }

    private UpdateEntry<String, String> generateRemoveMessage(String key, Timestamp timestamp) {
        return new UpdateEntry<>(key, new MapValue<>(null, timestamp));
    }

    private List<UpdateEntry<String, String>> generateRemoveMessage(String key1, String key2) {
        List<UpdateEntry<String, String>> list = new ArrayList<>();

        Timestamp timestamp1 = clockService.peek(1);
        Timestamp timestamp2 = clockService.peek(2);

        list.add(generateRemoveMessage(key1, timestamp1));
        list.add(generateRemoveMessage(key2, timestamp2));

        return list;
    }

    /**
     * Sets up a mock ClusterCommunicationService to expect a specific cluster
     * message to be broadcast to the cluster.
     *
     * @param message message we expect to be sent
     * @param clusterCommunicator a mock ClusterCommunicationService to set up
     */
    //FIXME rename
    private static <T> void expectSpecificBroadcastMessage(
            T message,
            MessageSubject subject,
            ClusterCommunicationService clusterCommunicator) {
        reset(clusterCommunicator);
        clusterCommunicator.<T>multicast(eq(message), eq(subject), anyObject(Function.class), anyObject(Set.class));
        expectLastCall().anyTimes();
        replay(clusterCommunicator);
    }

    /**
     * Sets up a mock ClusterCommunicationService to expect a specific cluster
     * message to be multicast to the cluster.
     *
     * @param message message we expect to be sent
     * @param subject subject we expect to be sent to
     * @param clusterCommunicator a mock ClusterCommunicationService to set up
     */
    //FIXME rename
    private static <T> void expectSpecificMulticastMessage(T message, MessageSubject subject,
                           ClusterCommunicationService clusterCommunicator) {
        reset(clusterCommunicator);
        clusterCommunicator.<T>multicast(eq(message), eq(subject), anyObject(Function.class), anyObject(Set.class));
        expectLastCall().anyTimes();
        replay(clusterCommunicator);
    }


    /**
     * Sets up a mock ClusterCommunicationService to expect a multicast cluster message
     * that is sent to it. This is useful for unit tests where we aren't
     * interested in testing the messaging component.
     *
     * @param clusterCommunicator a mock ClusterCommunicationService to set up
     */
    //FIXME rename
    private <T> void expectPeerMessage(ClusterCommunicationService clusterCommunicator) {
        reset(clusterCommunicator);
//        expect(clusterCommunicator.multicast(anyObject(ClusterMessage.class),
//                                             anyObject(Iterable.class)))
        expect(clusterCommunicator.<T>unicast(
                    anyObject(),
                    anyObject(MessageSubject.class),
                    anyObject(Function.class),
                    anyObject(NodeId.class)))
                .andReturn(CompletableFuture.completedFuture(null))
                .anyTimes();
        replay(clusterCommunicator);
    }

    /**
     * Sets up a mock ClusterCommunicationService to expect a broadcast cluster message
     * that is sent to it. This is useful for unit tests where we aren't
     * interested in testing the messaging component.
     *
     * @param clusterCommunicator a mock ClusterCommunicationService to set up
     */
    private void expectBroadcastMessage(ClusterCommunicationService clusterCommunicator) {
        reset(clusterCommunicator);
        clusterCommunicator.<AbstractEvent>multicast(
                anyObject(AbstractEvent.class),
                anyObject(MessageSubject.class),
                anyObject(Function.class),
                anyObject(Set.class));
        expectLastCall().anyTimes();
        replay(clusterCommunicator);
    }

    /**
     * ClusterCommunicationService implementation that the map's addSubscriber
     * call will delegate to. This means we can get a reference to the
     * internal cluster message handler used by the map, so that we can simulate
     * events coming in from other instances.
     */
    private final class TestClusterCommunicationService
            extends ClusterCommunicationServiceAdapter {

        @Override
        public <M> void addSubscriber(MessageSubject subject,
                Function<byte[], M> decoder, Consumer<M> handler,
                Executor executor) {
            if (subject.equals(UPDATE_MESSAGE_SUBJECT)) {
                updateHandler = (Consumer<Collection<UpdateEntry<String, String>>>) handler;
            } else if (subject.equals(UPDATE_REQUEST_SUBJECT)) {
                requestHandler = (Consumer<Collection<UpdateRequest<String>>>) handler;
            } else {
                throw new IllegalStateException("Unexpected message subject " + subject.toString());
            }
        }

        @Override
        public <M, R> void addSubscriber(MessageSubject subject,
                Function<byte[], M> decoder, Function<M, R> handler, Function<R, byte[]> encoder, Executor executor) {
            if (subject.equals(ANTI_ENTROPY_MESSAGE_SUBJECT)) {
                antiEntropyHandler = (Function<AntiEntropyAdvertisement<String>, AntiEntropyResponse>) handler;
            } else if (!subject.equals(INITIALIZE_MESSAGE_SUBJECT)) {
                throw new IllegalStateException("Unexpected message subject " + subject.toString());
            }
        }
    }

    /**
     * ClockService implementation that gives out timestamps based on a
     * sequential counter. This clock service enables more control over the
     * timestamps that are given out, including being able to "turn back time"
     * to give out timestamps from the past.
     *
     * @param <T> Type that the clock service will give out timestamps for
     * @param <U> Second type that the clock service will give out values for
     */
    private class SequentialClockService<T, U> {

        private static final long INITIAL_VALUE = 1;
        private final AtomicLong counter = new AtomicLong(INITIAL_VALUE);

        public Timestamp getTimestamp(T object, U object2) {
            return new TestTimestamp(counter.getAndIncrement());
        }

        /**
         * Returns what the next timestamp will be without consuming the
         * timestamp. This allows test code to set expectations correctly while
         * still allowing the CUT to get the same timestamp.
         *
         * @return timestamp equal to the timestamp that will be returned by the
         * next call to {@link #getTimestamp(T, U)}.
         */
        public Timestamp peekAtNextTimestamp() {
            return peek(1);
        }

        /**
         * Returns the ith timestamp to be given out in the future without
         * consuming the timestamp. For example, i=1 returns the next timestamp,
         * i=2 returns the timestamp after that, and so on.
         *
         * @param i number of the timestamp to peek at
         * @return the ith timestamp that will be given out
         */
        public Timestamp peek(int i) {
            checkArgument(i > 0, "i must be a positive integer");

            return new TestTimestamp(counter.get() + i - 1);
        }

        /**
         * Turns the clock back two ticks, so the next call to getTimestamp will
         * return an older timestamp than the previous call to getTimestamp.
         */
        public void turnBackTime() {
            // Not atomic, but should be OK for these tests.
            counter.decrementAndGet();
            counter.decrementAndGet();
        }

    }

    /**
     * Timestamp implementation where the value of the timestamp can be
     * specified explicitly at creation time.
     */
    private class TestTimestamp implements Timestamp {

        private final long timestamp;

        /**
         * Creates a new timestamp that has the specified value.
         *
         * @param timestamp value of the timestamp
         */
        public TestTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(Timestamp o) {
            checkArgument(o instanceof TestTimestamp);
            TestTimestamp otherTimestamp = (TestTimestamp) o;
            return ComparisonChain.start()
                    .compare(this.timestamp, otherTimestamp.timestamp)
                    .result();
        }
    }

    /**
     * EventuallyConsistentMapListener implementation which triggers a latch
     * when it receives an event.
     */
    private class TestListener implements EventuallyConsistentMapListener<String, String> {
        private CountDownLatch latch;

        /**
         * Creates a new listener that will trigger the specified latch when it
         * receives and event.
         *
         * @param latch the latch to trigger on events
         */
        public TestListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void event(EventuallyConsistentMapEvent<String, String> event) {
            latch.countDown();
        }
    }
}
