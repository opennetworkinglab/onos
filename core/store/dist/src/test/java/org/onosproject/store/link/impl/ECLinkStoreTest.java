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
package org.onosproject.store.link.impl;

import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Link.Type;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.net.device.DeviceClockServiceAdapter;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.link.LinkStoreDelegate;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Timestamp;
import org.onosproject.store.cluster.StaticClusterService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.impl.MastershipBasedTimestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.onosproject.cluster.ControllerNode.State.ACTIVE;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.Type.EDGE;
import static org.onosproject.net.Link.Type.INDIRECT;
import static org.onosproject.net.NetTestTools.assertAnnotationsEquals;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_UPDATED;

/**
 * Test of the GossipLinkStoreTest implementation.
 */
@Ignore
public class ECLinkStoreTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "bar", true);
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);

    private static final SparseAnnotations A1 = DefaultAnnotations.builder()
            .set("A1", "a1")
            .set("B1", "b1")
            .build();
    private static final SparseAnnotations A1_2 = DefaultAnnotations.builder()
            .remove("A1")
            .set("B3", "b3")
            .build();
    private static final SparseAnnotations A2 = DefaultAnnotations.builder()
            .set("A2", "a2")
            .set("B2", "b2")
            .build();
    private static final SparseAnnotations A2_2 = DefaultAnnotations.builder()
            .remove("A2")
            .set("B4", "b4")
            .build();

    // local node
    private static final NodeId NID1 = new NodeId("local");
    private static final ControllerNode ONOS1 =
            new DefaultControllerNode(NID1, IpAddress.valueOf("127.0.0.1"));

    // remote node
    private static final NodeId NID2 = new NodeId("remote");
    private static final ControllerNode ONOS2 =
            new DefaultControllerNode(NID2, IpAddress.valueOf("127.0.0.2"));

    private ECLinkStore linkStoreImpl;
    private LinkStore linkStore;

    private DeviceClockService deviceClockService;
    private ClusterCommunicationService clusterCommunicator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // TODO mock clusterCommunicator
        clusterCommunicator = createNiceMock(ClusterCommunicationService.class);
        clusterCommunicator.addSubscriber(anyObject(MessageSubject.class),
                                    anyObject(ClusterMessageHandler.class),
                                    anyObject(ExecutorService.class));
        expectLastCall().anyTimes();
        replay(clusterCommunicator);

        linkStoreImpl = new ECLinkStore();
        linkStoreImpl.deviceClockService = deviceClockService;
        linkStoreImpl.clusterCommunicator = clusterCommunicator;
        linkStoreImpl.clusterService = new TestClusterService();
        linkStoreImpl.deviceClockService = new TestDeviceClockService();
        linkStoreImpl.mastershipService = new TestMastershipService();
        linkStoreImpl.activate();
        linkStore = linkStoreImpl;

        verify(clusterCommunicator);
        reset(clusterCommunicator);

    }

    @After
    public void tearDown() throws Exception {
        linkStoreImpl.deactivate();
    }

    private void putLink(DeviceId srcId, PortNumber srcNum,
                         DeviceId dstId, PortNumber dstNum, Type type,
                         SparseAnnotations... annotations) {
        ConnectPoint src = new ConnectPoint(srcId, srcNum);
        ConnectPoint dst = new ConnectPoint(dstId, dstNum);
        linkStore.createOrUpdateLink(PID, new DefaultLinkDescription(src, dst, type, annotations));
        verify(clusterCommunicator);
    }

    private void putLink(LinkKey key, Type type, SparseAnnotations... annotations) {
        putLink(key.src().deviceId(), key.src().port(),
                key.dst().deviceId(), key.dst().port(),
                type, annotations);
    }

    private static void assertLink(DeviceId srcId, PortNumber srcNum,
                            DeviceId dstId, PortNumber dstNum, Type type,
                            Link link) {
        assertEquals(srcId, link.src().deviceId());
        assertEquals(srcNum, link.src().port());
        assertEquals(dstId, link.dst().deviceId());
        assertEquals(dstNum, link.dst().port());
        assertEquals(type, link.type());
    }

    private static void assertLink(LinkKey key, Type type, Link link) {
        assertLink(key.src().deviceId(), key.src().port(),
                   key.dst().deviceId(), key.dst().port(),
                   type, link);
    }

    @Test
    public final void testGetLinkCount() {
        assertEquals("initialy empty", 0, linkStore.getLinkCount());

        putLink(DID1, P1, DID2, P2, DIRECT);
        putLink(DID2, P2, DID1, P1, DIRECT);
        putLink(DID1, P1, DID2, P2, DIRECT);

        assertEquals("expecting 2 unique link", 2, linkStore.getLinkCount());
    }

    @Test
    public final void testGetLinks() {
        assertEquals("initialy empty", 0,
                Iterables.size(linkStore.getLinks()));

        LinkKey linkId1 = LinkKey.linkKey(new ConnectPoint(DID1, P1), new ConnectPoint(DID2, P2));
        LinkKey linkId2 = LinkKey.linkKey(new ConnectPoint(DID2, P2), new ConnectPoint(DID1, P1));

        putLink(linkId1, DIRECT);
        putLink(linkId2, DIRECT);
        putLink(linkId1, DIRECT);

        assertEquals("expecting 2 unique link", 2,
                Iterables.size(linkStore.getLinks()));

        Map<LinkKey, Link> links = new HashMap<>();
        for (Link link : linkStore.getLinks()) {
            links.put(LinkKey.linkKey(link), link);
        }

        assertLink(linkId1, DIRECT, links.get(linkId1));
        assertLink(linkId2, DIRECT, links.get(linkId2));
    }

    @Test
    public final void testGetDeviceEgressLinks() {
        LinkKey linkId1 = LinkKey.linkKey(new ConnectPoint(DID1, P1), new ConnectPoint(DID2, P2));
        LinkKey linkId2 = LinkKey.linkKey(new ConnectPoint(DID2, P2), new ConnectPoint(DID1, P1));
        LinkKey linkId3 = LinkKey.linkKey(new ConnectPoint(DID1, P2), new ConnectPoint(DID2, P3));

        putLink(linkId1, DIRECT);
        putLink(linkId2, DIRECT);
        putLink(linkId3, DIRECT);

        // DID1,P1 => DID2,P2
        // DID2,P2 => DID1,P1
        // DID1,P2 => DID2,P3

        Set<Link> links1 = linkStore.getDeviceEgressLinks(DID1);
        assertEquals(2, links1.size());
        // check

        Set<Link> links2 = linkStore.getDeviceEgressLinks(DID2);
        assertEquals(1, links2.size());
        assertLink(linkId2, DIRECT, links2.iterator().next());
    }

    @Test
    public final void testGetDeviceIngressLinks() {
        LinkKey linkId1 = LinkKey.linkKey(new ConnectPoint(DID1, P1), new ConnectPoint(DID2, P2));
        LinkKey linkId2 = LinkKey.linkKey(new ConnectPoint(DID2, P2), new ConnectPoint(DID1, P1));
        LinkKey linkId3 = LinkKey.linkKey(new ConnectPoint(DID1, P2), new ConnectPoint(DID2, P3));

        putLink(linkId1, DIRECT);
        putLink(linkId2, DIRECT);
        putLink(linkId3, DIRECT);

        // DID1,P1 => DID2,P2
        // DID2,P2 => DID1,P1
        // DID1,P2 => DID2,P3

        Set<Link> links1 = linkStore.getDeviceIngressLinks(DID2);
        assertEquals(2, links1.size());
        // check

        Set<Link> links2 = linkStore.getDeviceIngressLinks(DID1);
        assertEquals(1, links2.size());
        assertLink(linkId2, DIRECT, links2.iterator().next());
    }

    @Test
    public final void testGetLink() {
        ConnectPoint src = new ConnectPoint(DID1, P1);
        ConnectPoint dst = new ConnectPoint(DID2, P2);
        LinkKey linkId1 = LinkKey.linkKey(src, dst);

        putLink(linkId1, DIRECT);

        Link link = linkStore.getLink(src, dst);
        assertLink(linkId1, DIRECT, link);

        assertNull("There shouldn't be reverese link",
                linkStore.getLink(dst, src));
    }

    @Test
    public final void testGetEgressLinks() {
        final ConnectPoint d1P1 = new ConnectPoint(DID1, P1);
        final ConnectPoint d2P2 = new ConnectPoint(DID2, P2);
        LinkKey linkId1 = LinkKey.linkKey(d1P1, d2P2);
        LinkKey linkId2 = LinkKey.linkKey(d2P2, d1P1);
        LinkKey linkId3 = LinkKey.linkKey(new ConnectPoint(DID1, P2), new ConnectPoint(DID2, P3));

        putLink(linkId1, DIRECT);
        putLink(linkId2, DIRECT);
        putLink(linkId3, DIRECT);

        // DID1,P1 => DID2,P2
        // DID2,P2 => DID1,P1
        // DID1,P2 => DID2,P3

        Set<Link> links1 = linkStore.getEgressLinks(d1P1);
        assertEquals(1, links1.size());
        assertLink(linkId1, DIRECT, links1.iterator().next());

        Set<Link> links2 = linkStore.getEgressLinks(d2P2);
        assertEquals(1, links2.size());
        assertLink(linkId2, DIRECT, links2.iterator().next());
    }

    @Test
    public final void testGetIngressLinks() {
        final ConnectPoint d1P1 = new ConnectPoint(DID1, P1);
        final ConnectPoint d2P2 = new ConnectPoint(DID2, P2);
        LinkKey linkId1 = LinkKey.linkKey(d1P1, d2P2);
        LinkKey linkId2 = LinkKey.linkKey(d2P2, d1P1);
        LinkKey linkId3 = LinkKey.linkKey(new ConnectPoint(DID1, P2), new ConnectPoint(DID2, P3));

        putLink(linkId1, DIRECT);
        putLink(linkId2, DIRECT);
        putLink(linkId3, DIRECT);

        // DID1,P1 => DID2,P2
        // DID2,P2 => DID1,P1
        // DID1,P2 => DID2,P3

        Set<Link> links1 = linkStore.getIngressLinks(d2P2);
        assertEquals(1, links1.size());
        assertLink(linkId1, DIRECT, links1.iterator().next());

        Set<Link> links2 = linkStore.getIngressLinks(d1P1);
        assertEquals(1, links2.size());
        assertLink(linkId2, DIRECT, links2.iterator().next());
    }

    @Test
    public final void testCreateOrUpdateLink() {
        ConnectPoint src = new ConnectPoint(DID1, P1);
        ConnectPoint dst = new ConnectPoint(DID2, P2);

        final DefaultLinkDescription linkDescription = new DefaultLinkDescription(src, dst, INDIRECT);
        LinkEvent event = linkStore.createOrUpdateLink(PID,
                    linkDescription);

        assertLink(DID1, P1, DID2, P2, INDIRECT, event.subject());
        assertEquals(LINK_ADDED, event.type());

        LinkEvent event2 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, DIRECT));

        assertLink(DID1, P1, DID2, P2, DIRECT, event2.subject());
        assertEquals(LINK_UPDATED, event2.type());

        // no change
        LinkEvent event3 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, DIRECT));

        assertNull("No change event expected", event3);
    }

    private static void assertLinkDescriptionEquals(ConnectPoint src,
                                             ConnectPoint dst,
                                             Type type,
                                             LinkDescription actual) {
        assertEquals(src, actual.src());
        assertEquals(dst, actual.dst());
        assertEquals(type, actual.type());
        // TODO check annotations
    }

    @Test
    public final void testCreateOrUpdateLinkAncillary() {
        ConnectPoint src = new ConnectPoint(DID1, P1);
        ConnectPoint dst = new ConnectPoint(DID2, P2);

        // add Ancillary link
        LinkEvent event = linkStore.createOrUpdateLink(PIDA,
                    new DefaultLinkDescription(src, dst, INDIRECT, A1));

        assertNotNull("Ancillary only link is ignored", event);

        // add Primary link
        LinkEvent event2 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, INDIRECT, A2));

        assertLink(DID1, P1, DID2, P2, INDIRECT, event2.subject());
        assertAnnotationsEquals(event2.subject().annotations(), A2, A1);
        assertEquals(LINK_UPDATED, event2.type());

        // update link type
        LinkEvent event3 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, DIRECT, A2));

        assertLink(DID1, P1, DID2, P2, DIRECT, event3.subject());
        assertAnnotationsEquals(event3.subject().annotations(), A2, A1);
        assertEquals(LINK_UPDATED, event3.type());


        // no change
        LinkEvent event4 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, DIRECT));

        assertNull("No change event expected", event4);

        // update link annotation (Primary)
        LinkEvent event5 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, DIRECT, A2_2));

        assertLink(DID1, P1, DID2, P2, DIRECT, event5.subject());
        assertAnnotationsEquals(event5.subject().annotations(), A2, A2_2, A1);
        assertEquals(LINK_UPDATED, event5.type());

        // update link annotation (Ancillary)
        LinkEvent event6 = linkStore.createOrUpdateLink(PIDA,
                new DefaultLinkDescription(src, dst, DIRECT, A1_2));

        assertLink(DID1, P1, DID2, P2, DIRECT, event6.subject());
        assertAnnotationsEquals(event6.subject().annotations(), A2, A2_2, A1, A1_2);
        assertEquals(LINK_UPDATED, event6.type());

        // update link type (Ancillary) : ignored
        LinkEvent event7 = linkStore.createOrUpdateLink(PIDA,
                new DefaultLinkDescription(src, dst, EDGE));
        assertNull("Ancillary change other than annotation is ignored", event7);
    }


    @Test
    public final void testRemoveLink() {
        final ConnectPoint d1P1 = new ConnectPoint(DID1, P1);
        final ConnectPoint d2P2 = new ConnectPoint(DID2, P2);
        LinkKey linkId1 = LinkKey.linkKey(d1P1, d2P2);
        LinkKey linkId2 = LinkKey.linkKey(d2P2, d1P1);

        putLink(linkId1, DIRECT, A1);
        putLink(linkId2, DIRECT, A2);

        // DID1,P1 => DID2,P2
        // DID2,P2 => DID1,P1
        // DID1,P2 => DID2,P3

        LinkEvent event = linkStore.removeLink(d1P1, d2P2);
        assertEquals(LINK_REMOVED, event.type());
        assertAnnotationsEquals(event.subject().annotations(), A1);
        LinkEvent event2 = linkStore.removeLink(d1P1, d2P2);
        assertNull(event2);

        assertLink(linkId2, DIRECT, linkStore.getLink(d2P2, d1P1));
        assertAnnotationsEquals(linkStore.getLink(d2P2, d1P1).annotations(), A2);

        // annotations, etc. should not survive remove
        putLink(linkId1, DIRECT);
        assertLink(linkId1, DIRECT, linkStore.getLink(d1P1, d2P2));
        assertAnnotationsEquals(linkStore.getLink(d1P1, d2P2).annotations());
    }

    @Test
    public final void testAncillaryVisible() {
        ConnectPoint src = new ConnectPoint(DID1, P1);
        ConnectPoint dst = new ConnectPoint(DID2, P2);

        // add Ancillary link
        linkStore.createOrUpdateLink(PIDA,
                    new DefaultLinkDescription(src, dst, INDIRECT, A1));

        // Ancillary only link should not be visible
        assertEquals(1, linkStore.getLinkCount());
        assertNotNull(linkStore.getLink(src, dst));
    }

    // If Delegates should be called only on remote events,
    // then Simple* should never call them, thus not test required.
    @Ignore("Ignore until Delegate spec. is clear.")
    @Test
    public final void testEvents() throws InterruptedException {

        final ConnectPoint d1P1 = new ConnectPoint(DID1, P1);
        final ConnectPoint d2P2 = new ConnectPoint(DID2, P2);
        final LinkKey linkId1 = LinkKey.linkKey(d1P1, d2P2);

        final CountDownLatch addLatch = new CountDownLatch(1);
        LinkStoreDelegate checkAdd = event -> {
            assertEquals(LINK_ADDED, event.type());
            assertLink(linkId1, INDIRECT, event.subject());
            addLatch.countDown();
        };
        final CountDownLatch updateLatch = new CountDownLatch(1);
        LinkStoreDelegate checkUpdate = event -> {
            assertEquals(LINK_UPDATED, event.type());
            assertLink(linkId1, DIRECT, event.subject());
            updateLatch.countDown();
        };
        final CountDownLatch removeLatch = new CountDownLatch(1);
        LinkStoreDelegate checkRemove = event -> {
            assertEquals(LINK_REMOVED, event.type());
            assertLink(linkId1, DIRECT, event.subject());
            removeLatch.countDown();
        };

        linkStore.setDelegate(checkAdd);
        putLink(linkId1, INDIRECT);
        assertTrue("Add event fired", addLatch.await(1, TimeUnit.SECONDS));

        linkStore.unsetDelegate(checkAdd);
        linkStore.setDelegate(checkUpdate);
        putLink(linkId1, DIRECT);
        assertTrue("Update event fired", updateLatch.await(1, TimeUnit.SECONDS));

        linkStore.unsetDelegate(checkUpdate);
        linkStore.setDelegate(checkRemove);
        linkStore.removeLink(d1P1, d2P2);
        assertTrue("Remove event fired", removeLatch.await(1, TimeUnit.SECONDS));
    }

    private static final class TestClusterService extends StaticClusterService {

        public TestClusterService() {
            localNode = ONOS1;
            nodes.put(NID1, ONOS1);
            nodeStates.put(NID1, ACTIVE);

            nodes.put(NID2, ONOS2);
            nodeStates.put(NID2, ACTIVE);
        }
    }

    private final class TestDeviceClockService extends DeviceClockServiceAdapter {

        private final AtomicLong ticker = new AtomicLong();

        @Override
        public Timestamp getTimestamp(DeviceId deviceId) {
            if (DID1.equals(deviceId)) {
                return new MastershipBasedTimestamp(1, ticker.getAndIncrement());
            } else if (DID2.equals(deviceId)) {
                return new MastershipBasedTimestamp(2, ticker.getAndIncrement());
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public boolean isTimestampAvailable(DeviceId deviceId) {
            return DID1.equals(deviceId) || DID2.equals(deviceId);
        }
    }

    private final class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return NID1;
        }
    }
}
