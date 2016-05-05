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

package org.onosproject.ui.impl.topo.model;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.event.Event;
import org.onosproject.event.EventDispatcher;
import org.onosproject.ui.impl.topo.model.UiModelEvent.Type;
import org.onosproject.ui.model.topo.UiElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.cluster.NodeId.nodeId;

/**
 * Unit tests for {@link ModelCache}.
 */
public class ModelCacheTest extends AbstractTopoModelTest {

    private class TestEvDisp implements EventDispatcher {

        private Event<Type, UiElement> lastEvent = null;
        private int eventCount = 0;

        @Override
        public void post(Event event) {
            lastEvent = event;
            eventCount++;
//            print("Event dispatched: %s", event);
        }

        private void assertEventCount(int exp) {
            assertEquals("unex event count", exp, eventCount);
        }

        private void assertLast(Type expEventType, String expId) {
            assertNotNull("no last event", lastEvent);
            assertEquals("unex event type", expEventType, lastEvent.type());
            assertEquals("unex element ID", expId, lastEvent.subject().idAsString());
        }
    }

    private static IpAddress ip(String s) {
        return IpAddress.valueOf(s);
    }

    private static ControllerNode cnode(String id, String ip) {
        return new DefaultControllerNode(nodeId(id), ip(ip));
    }

    private static final String C1 = "C1";
    private static final String C2 = "C2";
    private static final String C3 = "C3";

    private static final ControllerNode NODE_1 = cnode(C1, "10.0.0.1");
    private static final ControllerNode NODE_2 = cnode(C2, "10.0.0.2");
    private static final ControllerNode NODE_3 = cnode(C3, "10.0.0.3");


    private final TestEvDisp dispatcher = new TestEvDisp();

    private ModelCache cache;

    @Before
    public void setUp() {
        cache = new ModelCache(MOCK_SERVICES, dispatcher);
    }

    @Test
    public void basic() {
        title("basic");
        print(cache);
        assertEquals("unex # members", 0, cache.clusterMemberCount());
        assertEquals("unex # regions", 0, cache.regionCount());
    }

    @Test
    public void addAndRemoveClusterMember() {
        title("addAndRemoveClusterMember");
        print(cache);
        assertEquals("unex # members", 0, cache.clusterMemberCount());
        dispatcher.assertEventCount(0);

        cache.addOrUpdateClusterMember(NODE_1);
        print(cache);
        assertEquals("unex # members", 1, cache.clusterMemberCount());
        dispatcher.assertEventCount(1);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C1);

        cache.removeClusterMember(NODE_1);
        print(cache);
        assertEquals("unex # members", 0, cache.clusterMemberCount());
        dispatcher.assertEventCount(2);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_REMOVED, C1);
    }

    @Test
    public void createThreeNodeCluster() {
        title("createThreeNodeCluster");
        cache.addOrUpdateClusterMember(NODE_1);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C1);
        cache.addOrUpdateClusterMember(NODE_2);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C2);
        cache.addOrUpdateClusterMember(NODE_3);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C3);
        dispatcher.assertEventCount(3);
        print(cache);
    }

}
