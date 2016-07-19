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

import io.atomix.Atomix;
import io.atomix.AtomixClient;
import io.atomix.resource.ResourceType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.Change;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AtomixLeaderElector}.
 */
public class AtomixLeaderElectorTest extends AtomixTestBase {

    NodeId node1 = new NodeId("node1");
    NodeId node2 = new NodeId("node2");
    NodeId node3 = new NodeId("node3");

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
        return new ResourceType(AtomixLeaderElector.class);
    }

    @Test
    public void testRun() throws Throwable {
        leaderElectorRunTests();
    }

    private void leaderElectorRunTests() throws Throwable {
        Atomix client1 = createAtomixClient();
        AtomixLeaderElector elector1 = client1.getResource("test-elector-run",
                                                           AtomixLeaderElector.class).join();
        elector1.run("foo", node1).thenAccept(result -> {
            assertEquals(node1, result.leaderNodeId());
            assertEquals(1, result.leader().term());
            assertEquals(1, result.candidates().size());
            assertEquals(node1, result.candidates().get(0));
        }).join();
        Atomix client2 = createAtomixClient();
        AtomixLeaderElector elector2 = client2.getResource("test-elector-run",
                                                           AtomixLeaderElector.class).join();
        elector2.run("foo", node2).thenAccept(result -> {
            assertEquals(node1, result.leaderNodeId());
            assertEquals(1, result.leader().term());
            assertEquals(2, result.candidates().size());
            assertEquals(node1, result.candidates().get(0));
            assertEquals(node2, result.candidates().get(1));
        }).join();
    }

    @Test
    public void testWithdraw() throws Throwable {
        leaderElectorWithdrawTests();
    }

    private void leaderElectorWithdrawTests() throws Throwable {
        Atomix client1 = createAtomixClient();
        AtomixLeaderElector elector1 = client1.getResource("test-elector-withdraw",
                                                           AtomixLeaderElector.class).join();
        elector1.run("foo", node1).join();
        Atomix client2 = createAtomixClient();
        AtomixLeaderElector elector2 = client2.getResource("test-elector-withdraw",
                                                           AtomixLeaderElector.class).join();
        elector2.run("foo", node2).join();

        LeaderEventListener listener1 = new LeaderEventListener();
        elector1.addChangeListener(listener1).join();

        LeaderEventListener listener2 = new LeaderEventListener();
        elector2.addChangeListener(listener2).join();

        elector1.withdraw("foo").join();

        listener1.nextEvent().thenAccept(result -> {
            assertEquals(node2, result.newValue().leaderNodeId());
            assertEquals(2, result.newValue().leader().term());
            assertEquals(1, result.newValue().candidates().size());
            assertEquals(node2, result.newValue().candidates().get(0));
        }).join();

        listener2.nextEvent().thenAccept(result -> {
            assertEquals(node2, result.newValue().leaderNodeId());
            assertEquals(2, result.newValue().leader().term());
            assertEquals(1, result.newValue().candidates().size());
            assertEquals(node2, result.newValue().candidates().get(0));
        }).join();
    }

    @Test
    public void testAnoint() throws Throwable {
        leaderElectorAnointTests();
    }

    private void leaderElectorAnointTests() throws Throwable {
        Atomix client1 = createAtomixClient();
        AtomixLeaderElector elector1 = client1.getResource("test-elector-anoint",
                                                           AtomixLeaderElector.class).join();
        Atomix client2 = createAtomixClient();
        AtomixLeaderElector elector2 = client2.getResource("test-elector-anoint",
                                                           AtomixLeaderElector.class).join();
        Atomix client3 = createAtomixClient();
        AtomixLeaderElector elector3 = client3.getResource("test-elector-anoint",
                                                           AtomixLeaderElector.class).join();
        elector1.run("foo", node1).join();
        elector2.run("foo", node2).join();

        LeaderEventListener listener1 = new LeaderEventListener();
        elector1.addChangeListener(listener1).join();
        LeaderEventListener listener2 = new LeaderEventListener();
        elector2.addChangeListener(listener2);
        LeaderEventListener listener3 = new LeaderEventListener();
        elector3.addChangeListener(listener3).join();

        elector3.anoint("foo", node3).thenAccept(result -> {
            assertFalse(result);
        }).join();
        assertFalse(listener1.hasEvent());
        assertFalse(listener2.hasEvent());
        assertFalse(listener3.hasEvent());

        elector3.anoint("foo", node2).thenAccept(result -> {
            assertTrue(result);
        }).join();

        listener1.nextEvent().thenAccept(result -> {
            assertEquals(node2, result.newValue().leaderNodeId());
            assertEquals(2, result.newValue().candidates().size());
            assertEquals(node1, result.newValue().candidates().get(0));
            assertEquals(node2, result.newValue().candidates().get(1));
        }).join();
        listener2.nextEvent().thenAccept(result -> {
            assertEquals(node2, result.newValue().leaderNodeId());
            assertEquals(2, result.newValue().candidates().size());
            assertEquals(node1, result.newValue().candidates().get(0));
            assertEquals(node2, result.newValue().candidates().get(1));
        }).join();
        listener3.nextEvent().thenAccept(result -> {
            assertEquals(node2, result.newValue().leaderNodeId());
            assertEquals(2, result.newValue().candidates().size());
            assertEquals(node1, result.newValue().candidates().get(0));
            assertEquals(node2, result.newValue().candidates().get(1));
        }).join();
    }

    @Test
    public void testPromote() throws Throwable {
        leaderElectorPromoteTests();
    }

    private void leaderElectorPromoteTests() throws Throwable {
        AtomixClient client1 = createAtomixClient();
        AtomixLeaderElector elector1 = client1.getResource("test-elector-promote",
                                                           AtomixLeaderElector.class).join();
        AtomixClient client2 = createAtomixClient();
        AtomixLeaderElector elector2 = client2.getResource("test-elector-promote",
                                                           AtomixLeaderElector.class).join();
        AtomixClient client3 = createAtomixClient();
        AtomixLeaderElector elector3 = client3.getResource("test-elector-promote",
                                                           AtomixLeaderElector.class).join();
        elector1.run("foo", node1).join();
        elector2.run("foo", node2).join();

        LeaderEventListener listener1 = new LeaderEventListener();
        elector1.addChangeListener(listener1).join();
        LeaderEventListener listener2 = new LeaderEventListener();
        elector2.addChangeListener(listener2).join();
        LeaderEventListener listener3 = new LeaderEventListener();
        elector3.addChangeListener(listener3).join();

        elector3.promote("foo", node3).thenAccept(result -> {
            assertFalse(result);
        }).join();

        assertFalse(listener1.hasEvent());
        assertFalse(listener2.hasEvent());
        assertFalse(listener3.hasEvent());

        elector3.run("foo", node3).join();

        listener1.nextEvent().thenAccept(result -> {
            assertEquals(node3, result.newValue().candidates().get(2));
        }).join();
        listener2.nextEvent().thenAccept(result -> {
            assertEquals(node3, result.newValue().candidates().get(2));
        }).join();
        listener3.nextEvent().thenAccept(result -> {
            assertEquals(node3, result.newValue().candidates().get(2));
        }).join();

        elector3.promote("foo", node3).thenAccept(result -> {
            assertTrue(result);
        }).join();

        listener1.nextEvent().thenAccept(result -> {
            assertEquals(node3, result.newValue().candidates().get(0));
        }).join();
        listener2.nextEvent().thenAccept(result -> {
            assertEquals(node3, result.newValue().candidates().get(0));
        }).join();
        listener3.nextEvent().thenAccept(result -> {
            assertEquals(node3, result.newValue().candidates().get(0));
        }).join();
    }

    @Test
    public void testLeaderSessionClose() throws Throwable {
        leaderElectorLeaderSessionCloseTests();
    }

    private void leaderElectorLeaderSessionCloseTests() throws Throwable {
        AtomixClient client1 = createAtomixClient();
        AtomixLeaderElector elector1 = client1.getResource("test-elector-leader-session-close",
                                                           AtomixLeaderElector.class).join();
        elector1.run("foo", node1).join();
        Atomix client2 = createAtomixClient();
        AtomixLeaderElector elector2 = client2.getResource("test-elector-leader-session-close",
                                                           AtomixLeaderElector.class).join();
        LeaderEventListener listener = new LeaderEventListener();
        elector2.run("foo", node2).join();
        elector2.addChangeListener(listener).join();
        client1.close();
        listener.nextEvent().thenAccept(result -> {
            assertEquals(node2, result.newValue().leaderNodeId());
            assertEquals(1, result.newValue().candidates().size());
            assertEquals(node2, result.newValue().candidates().get(0));
        }).join();
    }

    @Test
    public void testNonLeaderSessionClose() throws Throwable {
        leaderElectorNonLeaderSessionCloseTests();
    }

    private void leaderElectorNonLeaderSessionCloseTests() throws Throwable {
        Atomix client1 = createAtomixClient();
        AtomixLeaderElector elector1 = client1.getResource("test-elector-non-leader-session-close",
                                                           AtomixLeaderElector.class).join();
        elector1.run("foo", node1).join();
        AtomixClient client2 = createAtomixClient();
        AtomixLeaderElector elector2 = client2.getResource("test-elector-non-leader-session-close",
                                                           AtomixLeaderElector.class).join();
        LeaderEventListener listener = new LeaderEventListener();
        elector2.run("foo", node2).join();
        elector1.addChangeListener(listener).join();
        client2.close().join();
        listener.nextEvent().thenAccept(result -> {
            assertEquals(node1, result.newValue().leaderNodeId());
            assertEquals(1, result.newValue().candidates().size());
            assertEquals(node1, result.newValue().candidates().get(0));
        }).join();
    }

    @Test
    public void testQueries() throws Throwable {
        leaderElectorQueryTests();
    }

    private void leaderElectorQueryTests() throws Throwable {
        Atomix client1 = createAtomixClient();
        Atomix client2 = createAtomixClient();
        AtomixLeaderElector elector1 = client1.getResource("test-elector-query",
                                                           AtomixLeaderElector.class).join();
        AtomixLeaderElector elector2 = client2.getResource("test-elector-query",
                                                           AtomixLeaderElector.class).join();
        elector1.run("foo", node1).join();
        elector2.run("foo", node2).join();
        elector2.run("bar", node2).join();
        elector1.getElectedTopics(node1).thenAccept(result -> {
            assertEquals(1, result.size());
            assertTrue(result.contains("foo"));
        }).join();
        elector2.getElectedTopics(node1).thenAccept(result -> {
            assertEquals(1, result.size());
            assertTrue(result.contains("foo"));
        }).join();
        elector1.getLeadership("foo").thenAccept(result -> {
            assertEquals(node1, result.leaderNodeId());
            assertEquals(node1, result.candidates().get(0));
            assertEquals(node2, result.candidates().get(1));
        }).join();
        elector2.getLeadership("foo").thenAccept(result -> {
            assertEquals(node1, result.leaderNodeId());
            assertEquals(node1, result.candidates().get(0));
            assertEquals(node2, result.candidates().get(1));
        }).join();
        elector1.getLeadership("bar").thenAccept(result -> {
            assertEquals(node2, result.leaderNodeId());
            assertEquals(node2, result.candidates().get(0));
        }).join();
        elector2.getLeadership("bar").thenAccept(result -> {
            assertEquals(node2, result.leaderNodeId());
            assertEquals(node2, result.candidates().get(0));
        }).join();
        elector1.getLeaderships().thenAccept(result -> {
            assertEquals(2, result.size());
            Leadership fooLeadership = result.get("foo");
            assertEquals(node1, fooLeadership.leaderNodeId());
            assertEquals(node1, fooLeadership.candidates().get(0));
            assertEquals(node2, fooLeadership.candidates().get(1));
            Leadership barLeadership = result.get("bar");
            assertEquals(node2, barLeadership.leaderNodeId());
            assertEquals(node2, barLeadership.candidates().get(0));
        }).join();
        elector2.getLeaderships().thenAccept(result -> {
            assertEquals(2, result.size());
            Leadership fooLeadership = result.get("foo");
            assertEquals(node1, fooLeadership.leaderNodeId());
            assertEquals(node1, fooLeadership.candidates().get(0));
            assertEquals(node2, fooLeadership.candidates().get(1));
            Leadership barLeadership = result.get("bar");
            assertEquals(node2, barLeadership.leaderNodeId());
            assertEquals(node2, barLeadership.candidates().get(0));
        }).join();
    }

    private static class LeaderEventListener implements Consumer<Change<Leadership>> {
        Queue<Change<Leadership>> eventQueue = new LinkedList<>();
        CompletableFuture<Change<Leadership>> pendingFuture;

        @Override
        public void accept(Change<Leadership> change) {
            synchronized (this) {
                if (pendingFuture != null) {
                    pendingFuture.complete(change);
                    pendingFuture = null;
                } else {
                    eventQueue.add(change);
                }
            }
        }

        public boolean hasEvent() {
            return !eventQueue.isEmpty();
        }

        public void clearEvents() {
            eventQueue.clear();
        }

        public CompletableFuture<Change<Leadership>> nextEvent() {
            synchronized (this) {
                if (eventQueue.isEmpty()) {
                    if (pendingFuture == null) {
                        pendingFuture = new CompletableFuture<>();
                    }
                    return pendingFuture;
                } else {
                    return CompletableFuture.completedFuture(eventQueue.poll());
                }
            }
        }
    }
}
