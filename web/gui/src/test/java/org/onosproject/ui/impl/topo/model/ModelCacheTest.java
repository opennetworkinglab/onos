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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.region.Region;
import org.onosproject.ui.impl.topo.model.UiModelEvent.Type;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiDeviceLink;
import org.onosproject.ui.model.topo.UiElement;
import org.onosproject.ui.model.topo.UiHost;
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.model.topo.UiRegion;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.cluster.NodeId.nodeId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.ui.model.topo.UiLinkId.uiLinkId;

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

    private void assertContains(String msg, Collection<?> coll, Object... things) {
        for (Object o : things) {
            assertTrue(msg, coll.contains(o));
        }
    }

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
    public void nonExistentClusterMember() {
        title("nonExistentClusterMember");
        cache.addOrUpdateClusterMember(CNODE_1);
        print(cache);
        assertEquals("unex # members", 1, cache.clusterMemberCount());
        dispatcher.assertEventCount(1);
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C1);

        cache.removeClusterMember(CNODE_2);
        assertEquals("unex # members", 1, cache.clusterMemberCount());
        dispatcher.assertEventCount(1);
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
        dispatcher.assertLast(Type.CLUSTER_MEMBER_ADDED_OR_UPDATED, C1);
        cache.addOrUpdateDevice(DEV_1);
        dispatcher.assertLast(Type.DEVICE_ADDED_OR_UPDATED, D1);
        cache.addOrUpdateDevice(DEV_2);
        dispatcher.assertLast(Type.DEVICE_ADDED_OR_UPDATED, D2);
        cache.addOrUpdateDevice(DEV_3);
        dispatcher.assertLast(Type.DEVICE_ADDED_OR_UPDATED, D3);
        dispatcher.assertEventCount(4);
        print(cache);

        assertEquals("unex # nodes", 1, cache.clusterMemberCount());
        assertEquals("unex # devices", 3, cache.deviceCount());
        cache.removeDevice(DEV_4);
        assertEquals("unex # devices", 3, cache.deviceCount());
        dispatcher.assertEventCount(4);

        cache.removeDevice(DEV_2);
        dispatcher.assertLast(Type.DEVICE_REMOVED, D2);
        dispatcher.assertEventCount(5);

        // check out details of device
        UiDevice dev = cache.accessDevice(DEVID_1);
        assertEquals("wrong id", D1, dev.idAsString());
        assertEquals("wrong region", R1, dev.regionId().toString());
        Device d = dev.backingDevice();
        assertEquals("wrong serial", SERIAL, d.serialNumber());
    }

    @Test
    public void addRegions() {
        title("addRegions");
        cache.addOrUpdateRegion(REGION_1);
        dispatcher.assertLast(Type.REGION_ADDED_OR_UPDATED, R1);
        dispatcher.assertEventCount(1);
        assertEquals("unex # regions", 1, cache.regionCount());

        cache.addOrUpdateRegion(REGION_2);
        dispatcher.assertLast(Type.REGION_ADDED_OR_UPDATED, R2);
        dispatcher.assertEventCount(2);
        assertEquals("unex # regions", 2, cache.regionCount());

        print(cache);

        cache.removeRegion(REGION_3);
        dispatcher.assertEventCount(2);
        assertEquals("unex # regions", 2, cache.regionCount());

        cache.removeRegion(REGION_1);
        dispatcher.assertLast(Type.REGION_REMOVED, R1);
        dispatcher.assertEventCount(3);
        assertEquals("unex # regions", 1, cache.regionCount());

        print(cache);

        UiRegion region = cache.accessRegion(REGION_2.id());
        assertEquals("wrong id", REGION_2.id(), region.id());
        assertEquals("unex # device IDs", 3, region.deviceIds().size());
        assertContains("missing ID", region.deviceIds(), DEVID_4, DEVID_5, DEVID_6);
        Region r = region.backingRegion();
        print(r);
        assertEquals("wrong region name", "Region-R2", r.name());
    }

    private static final String[] LINKS_2_7 = {D2, "27", D7, "72"};

    @Test
    public void addLinks() {
        title("addLinks");

        Iterator<Link> iter = makeLinkPair(LINKS_2_7).iterator();
        Link link1 = iter.next();
        Link link2 = iter.next();
        print(link1);
        print(link2);

        UiLinkId idA2B = uiLinkId(link1);
        UiLinkId idB2A = uiLinkId(link2);
        // remember, link IDs are canonicalized
        assertEquals("not same link ID", idA2B, idB2A);

        // we've established that the ID is the same for both
        UiLinkId linkId = idA2B;

        cache.addOrUpdateDeviceLink(link1);
        dispatcher.assertLast(Type.LINK_ADDED_OR_UPDATED, linkId.toString());
        dispatcher.assertEventCount(1);
        assertEquals("unex # links", 1, cache.deviceLinkCount());

        UiDeviceLink link = cache.accessDeviceLink(linkId);
        assertEquals("dev A not d2", DEVID_2, link.deviceA());
        assertEquals("dev B not d7", DEVID_7, link.deviceB());
        assertEquals("wrong backing link A-B", link1, link.linkAtoB());
        assertEquals("backing link B-A?", null, link.linkBtoA());

        cache.addOrUpdateDeviceLink(link2);
        dispatcher.assertLast(Type.LINK_ADDED_OR_UPDATED, linkId.toString());
        dispatcher.assertEventCount(2);
        // NOTE: yes! expect 1 UiLink
        assertEquals("unex # links", 1, cache.deviceLinkCount());

        link = cache.accessDeviceLink(linkId);
        assertEquals("dev A not d2", DEVID_2, link.deviceA());
        assertEquals("dev B not d7", DEVID_7, link.deviceB());
        assertEquals("wrong backing link A-B", link1, link.linkAtoB());
        assertEquals("wrong backing link B-A", link2, link.linkBtoA());

        // now remove links one at a time
        cache.removeDeviceLink(link1);
        // NOTE: yes! ADD_OR_UPDATE, since the link was updated
        dispatcher.assertLast(Type.LINK_ADDED_OR_UPDATED, linkId.toString());
        dispatcher.assertEventCount(3);
        // NOTE: yes! expect 1 UiLink (still)
        assertEquals("unex # links", 1, cache.deviceLinkCount());

        link = cache.accessDeviceLink(linkId);
        assertEquals("dev A not d2", DEVID_2, link.deviceA());
        assertEquals("dev B not d7", DEVID_7, link.deviceB());
        assertEquals("backing link A-B?", null, link.linkAtoB());
        assertEquals("wrong backing link B-A", link2, link.linkBtoA());

        // remove final link
        cache.removeDeviceLink(link2);
        dispatcher.assertLast(Type.LINK_REMOVED, linkId.toString());
        dispatcher.assertEventCount(4);
        // NOTE: finally link should be removed from cache
        assertEquals("unex # links", 0, cache.deviceLinkCount());
    }

    private void assertHostLinkCounts(int nHosts, int nLinks) {
        assertEquals("unex # hosts", nHosts, cache.hostCount());
        assertEquals("unex # links", nLinks, cache.edgeLinkCount());
    }

    private void assertLocation(HostId hid, DeviceId expDev, int expPort) {
        UiHost h = cache.accessHost(hid);
        assertEquals("unex device", expDev, h.locationDevice());
        assertEquals("unex port", portNumber(expPort), h.locationPort());
    }

    @Test
    public void addHosts() {
        title("addHosts");

        assertHostLinkCounts(0, 0);
        Host hostA = createHost(DEV_1, 101, "a");
        Host hostB = createHost(DEV_1, 102, "b");

        // add a host
        cache.addOrUpdateHost(hostA);
        dispatcher.assertLast(Type.HOST_ADDED_OR_UPDATED, hostA.id().toString());
        dispatcher.assertEventCount(1);
        assertHostLinkCounts(1, 1);
        assertLocation(hostA.id(), DEVID_1, 101);

        // add a second host
        cache.addOrUpdateHost(hostB);
        dispatcher.assertLast(Type.HOST_ADDED_OR_UPDATED, hostB.id().toString());
        dispatcher.assertEventCount(2);
        assertHostLinkCounts(2, 2);
        assertLocation(hostB.id(), DEVID_1, 102);

        // update the first host
        cache.addOrUpdateHost(hostA);
        dispatcher.assertLast(Type.HOST_ADDED_OR_UPDATED, hostA.id().toString());
        dispatcher.assertEventCount(3);
        assertHostLinkCounts(2, 2);
        assertLocation(hostA.id(), DEVID_1, 101);

        print(cache.dumpString());

        // remove the second host
        cache.removeHost(hostB);
        dispatcher.assertLast(Type.HOST_REMOVED, hostB.id().toString());
        dispatcher.assertEventCount(4);
        assertHostLinkCounts(1, 1);
        assertNull("still host B?", cache.accessHost(hostB.id()));

        print(cache.dumpString());

        // first, verify where host A is currently residing
        assertLocation(hostA.id(), DEVID_1, 101);

        // now let's move hostA to a different port
        Host movedHost = createHost(DEV_1, 200, "a");
        print(hostA);
        print(movedHost);

        cache.moveHost(movedHost, hostA);
        dispatcher.assertLast(Type.HOST_MOVED, hostA.id().toString());
        dispatcher.assertEventCount(5);
        assertHostLinkCounts(1, 1);

        assertLocation(hostA.id(), DEVID_1, 200);

        print(cache.dumpString());

        // finally, let's move the host to a different device and port
        Host movedAgain = createHost(DEV_8, 800, "a");

        cache.moveHost(movedAgain, movedHost);
        dispatcher.assertLast(Type.HOST_MOVED, hostA.id().toString());
        dispatcher.assertEventCount(6);
        assertHostLinkCounts(1, 1);

        assertLocation(hostA.id(), DEVID_8, 800);

        print(cache.dumpString());
    }


    @Test
    public void load() {
        title("load");
        cache.load();
        print(cache.dumpString());

        // See mock service bundle for expected values (AbstractTopoModelTest)
        assertEquals("unex # cnodes", 3, cache.clusterMemberCount());
        assertEquals("unex # regions", 3, cache.regionCount());
        assertEquals("unex # devices", 9, cache.deviceCount());
        assertEquals("unex # hosts", 18, cache.hostCount());
        assertEquals("unex # device-links", 8, cache.deviceLinkCount());
        assertEquals("unex # edge-links", 18, cache.edgeLinkCount());
        assertEquals("unex # synth-links", 0, cache.synthLinkCount());
    }
}
