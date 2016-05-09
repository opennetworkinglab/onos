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
import org.onosproject.event.Event;
import org.onosproject.event.EventDispatcher;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.impl.topo.model.UiModelEvent.Type;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

        cache.addOrUpdateClusterMember(CNODE_1);
        print(cache);
        assertEquals("unex # members", 1, cache.clusterMemberCount());
        dispatcher.assertEventCount(1);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C1);

        cache.removeClusterMember(CNODE_1);
        print(cache);
        assertEquals("unex # members", 0, cache.clusterMemberCount());
        dispatcher.assertEventCount(2);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_REMOVED, C1);
    }

    @Test
    public void createThreeNodeCluster() {
        title("createThreeNodeCluster");
        cache.addOrUpdateClusterMember(CNODE_1);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C1);
        cache.addOrUpdateClusterMember(CNODE_2);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C2);
        cache.addOrUpdateClusterMember(CNODE_3);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C3);
        dispatcher.assertEventCount(3);
        print(cache);
    }

    @Test
    public void addNodeThenExamineIt() {
        title("addNodeThenExamineIt");
        cache.addOrUpdateClusterMember(CNODE_1);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C1);

        UiClusterMember member = cache.accessClusterMember(nodeId(C1));
        print(member);
        // see AbstractUiImplTest Mock Environment for expected values...
        assertEquals("wrong id str", C1, member.idAsString());
        assertEquals("wrong id", nodeId(C1), member.id());
        assertEquals("wrong dev count", 3, member.deviceCount());
        assertEquals("not online", true, member.isOnline());
        assertEquals("not ready", true, member.isReady());

        assertMasterOf(member, DEVID_1, DEVID_2, DEVID_3);
        assertNotMasterOf(member, DEVID_4, DEVID_6, DEVID_9);
    }

    private void assertMasterOf(UiClusterMember member, DeviceId... ids) {
        for (DeviceId id : ids) {
            assertTrue("not master of " + id, member.masterOf(id));
        }
    }

    private void assertNotMasterOf(UiClusterMember member, DeviceId... ids) {
        for (DeviceId id : ids) {
            assertFalse("? master of " + id, member.masterOf(id));
        }
    }


    @Test
    public void addNodeAndDevices() {
        title("addNodeAndDevices");
        cache.addOrUpdateClusterMember(CNODE_1);
        cache.addOrUpdateDevice(DEV_1);
        cache.addOrUpdateDevice(DEV_2);
        cache.addOrUpdateDevice(DEV_3);
        print(cache);
    }

    @Test
    public void addRegions() {
        title("addRegions");
        cache.addOrUpdateRegion(REGION_1);
        print(cache);
    }

    @Test
    public void load() {
        title("load");
        cache.load();
        print(cache);
    }
}
