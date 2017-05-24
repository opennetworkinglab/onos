/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.trivial;

import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Link.Type;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.link.LinkStoreDelegate;
import org.onosproject.net.provider.ProviderId;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.*;
import static org.onosproject.net.link.LinkEvent.Type.*;
import static org.onosproject.net.NetTestTools.assertAnnotationsEquals;

/**
 * Test of the simple LinkStore implementation.
 */
public class SimpleLinkStoreTest {

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

    private static final SparseAnnotations DA1 = DefaultAnnotations.builder()
            .set("A1", "a1")
            .set("B1", "b1")
            .set(AnnotationKeys.DURABLE, "true")
            .build();
    private static final SparseAnnotations DA2 = DefaultAnnotations.builder()
            .set("A2", "a2")
            .set("B2", "b2")
            .set(AnnotationKeys.DURABLE, "true")
            .build();
    private static final SparseAnnotations NDA1 = DefaultAnnotations.builder()
            .set("A1", "a1")
            .set("B1", "b1")
            .remove(AnnotationKeys.DURABLE)
            .build();



    private SimpleLinkStore simpleLinkStore;
    private LinkStore linkStore;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        simpleLinkStore = new SimpleLinkStore();
        simpleLinkStore.activate();
        linkStore = simpleLinkStore;
    }

    @After
    public void tearDown() throws Exception {
        simpleLinkStore.deactivate();
    }

    private void putLink(DeviceId srcId, PortNumber srcNum,
                         DeviceId dstId, PortNumber dstNum,
                         Type type, boolean isDurable,
                         SparseAnnotations... annotations) {
        ConnectPoint src = new ConnectPoint(srcId, srcNum);
        ConnectPoint dst = new ConnectPoint(dstId, dstNum);
        linkStore.createOrUpdateLink(PID, new DefaultLinkDescription(src, dst, type,
                                                                     annotations));
    }

    private void putLink(LinkKey key, Type type, SparseAnnotations... annotations) {
        putLink(key.src().deviceId(), key.src().port(),
                key.dst().deviceId(), key.dst().port(),
                type, false, annotations);
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

        putLink(DID1, P1, DID2, P2, DIRECT, false);
        putLink(DID2, P2, DID1, P1, DIRECT, false);
        putLink(DID1, P1, DID2, P2, DIRECT, false);

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

        // add link
        LinkEvent event = linkStore.createOrUpdateLink(PID,
                    new DefaultLinkDescription(src, dst, INDIRECT));

        assertLink(DID1, P1, DID2, P2, INDIRECT, event.subject());
        assertEquals(LINK_ADDED, event.type());

        // update link type
        LinkEvent event2 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, DIRECT));

        assertLink(DID1, P1, DID2, P2, DIRECT, event2.subject());
        assertEquals(LINK_UPDATED, event2.type());

        // no change
        LinkEvent event3 = linkStore.createOrUpdateLink(PID,
                new DefaultLinkDescription(src, dst, DIRECT));

        assertNull("No change event expected", event3);
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
    public final void testRemoveOrDownLink() {
        removeOrDownLink(false);
    }

    @Test
    public final void testRemoveOrDownLinkDurable() {
        removeOrDownLink(true);
    }

    private void removeOrDownLink(boolean isDurable) {
        final ConnectPoint d1P1 = new ConnectPoint(DID1, P1);
        final ConnectPoint d2P2 = new ConnectPoint(DID2, P2);
        LinkKey linkId1 = LinkKey.linkKey(d1P1, d2P2);
        LinkKey linkId2 = LinkKey.linkKey(d2P2, d1P1);

        putLink(linkId1, DIRECT, isDurable ? DA1 : A1);
        putLink(linkId2, DIRECT, isDurable ? DA2 : A2);

        // DID1,P1 => DID2,P2
        // DID2,P2 => DID1,P1
        // DID1,P2 => DID2,P3

        LinkEvent event = linkStore.removeOrDownLink(d1P1, d2P2);
        assertEquals(isDurable ? LINK_UPDATED : LINK_REMOVED, event.type());
        assertAnnotationsEquals(event.subject().annotations(), isDurable ? DA1 : A1);
        LinkEvent event2 = linkStore.removeOrDownLink(d1P1, d2P2);
        assertNull(event2);

        assertLink(linkId2, DIRECT, linkStore.getLink(d2P2, d1P1));
        assertAnnotationsEquals(linkStore.getLink(d2P2, d1P1).annotations(),
                                isDurable ? DA2 : A2);

        // annotations, etc. should not survive remove
        if (!isDurable) {
            putLink(linkId1, DIRECT);
            assertLink(linkId1, DIRECT, linkStore.getLink(d1P1, d2P2));
            assertAnnotationsEquals(linkStore.getLink(d1P1, d2P2).annotations());
        }
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

    @Test
    public void testDurableToNonDurable() {
        final ConnectPoint d1P1 = new ConnectPoint(DID1, P1);
        final ConnectPoint d2P2 = new ConnectPoint(DID2, P2);
        LinkKey linkId1 = LinkKey.linkKey(d1P1, d2P2);

        putLink(linkId1, DIRECT, DA1);
        assertTrue("should be be durable", linkStore.getLink(d1P1, d2P2).isExpected());
        putLink(linkId1, DIRECT, NDA1);
        assertFalse("should not be durable", linkStore.getLink(d1P1, d2P2).isExpected());
    }

    @Test
    public void testNonDurableToDurable() {
        final ConnectPoint d1P1 = new ConnectPoint(DID1, P1);
        final ConnectPoint d2P2 = new ConnectPoint(DID2, P2);
        LinkKey linkId1 = LinkKey.linkKey(d1P1, d2P2);

        putLink(linkId1, DIRECT, A1);
        assertFalse("should not be durable", linkStore.getLink(d1P1, d2P2).isExpected());
        putLink(linkId1, DIRECT, DA1);
        assertTrue("should be durable", linkStore.getLink(d1P1, d2P2).isExpected());
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
        linkStore.removeOrDownLink(d1P1, d2P2);
        assertTrue("Remove event fired", removeLatch.await(1, TimeUnit.SECONDS));
    }
}
