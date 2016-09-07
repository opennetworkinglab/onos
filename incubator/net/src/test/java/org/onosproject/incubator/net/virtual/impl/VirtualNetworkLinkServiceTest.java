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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Iterators;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.store.service.TestStorageService;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Junit tests for VirtualNetworkLinkService.
 */
public class VirtualNetworkLinkServiceTest extends TestDeviceParams {

    private final String tenantIdValue1 = "TENANT_ID1";

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private TestableIntentService intentService = new FakeIntentManager();

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        coreService = new VirtualNetworkLinkServiceTest.TestCoreService();
        virtualNetworkManagerStore.setCoreService(coreService);
        TestUtils.setField(coreService, "coreService", new VirtualNetworkLinkServiceTest.TestCoreService());
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.intentService = intentService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.activate();
    }

    @After
    public void tearDown() {
        virtualNetworkManagerStore.deactivate();
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
    }

    /**
     * Tests the getLinks(), getActiveLinks(), getLinkCount(), getLink(),
     * getLinks(ConnectPoint), getDeviceLinks(), getDeviceEgressLinks(), getDeviceIngressLinks(),
     * getEgressLinks(), getIngressLinks() methods.
     */
    @Test
    public void testGetLinks() {

        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), src.deviceId(), src.port(),
                                  new DefaultPort(srcVirtualDevice, src.port(), true));

        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), dst.deviceId(), dst.port(),
                                  new DefaultPort(dstVirtualDevice, dst.port(), true));

        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), src, dst);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), dst, src);

        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getLinks() method
        Iterator<Link> it = linkService.getLinks().iterator();
        assertEquals("The link set size did not match.", 2, Iterators.size(it));

        // test the getActiveLinks() method where all links are INACTIVE
        Iterator<Link> it2 = linkService.getActiveLinks().iterator();
        assertEquals("The link set size did not match.", 0, Iterators.size(it2));

        // test the getActiveLinks() method where one link is ACTIVE
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        Iterator<Link> it3 = linkService.getActiveLinks().iterator();
        assertEquals("The link set size did not match.", 1, Iterators.size(it3));

        // test the getLinkCount() method
        assertEquals("The link set size did not match.", 2, linkService.getLinkCount());

        // test the getLink() method
        assertEquals("The expect link did not match.", link1,
                     linkService.getLink(src, dst));
        assertEquals("The expect link did not match.", link2,
                     linkService.getLink(dst, src));
        assertNotEquals("The expect link should not have matched.", link1,
                        linkService.getLink(dst, src));

        // test the getLinks(ConnectPoint) method
        assertEquals("The link set size did not match.", 2, linkService.getLinks(src).size());
        assertEquals("The link set size did not match.", 2, linkService.getLinks(dst).size());
        ConnectPoint connectPoint = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(3));
        assertEquals("The link set size did not match.", 0, linkService.getLinks(connectPoint).size());

        // test the getDeviceLinks() method
        assertEquals("The link set size did not match.", 2,
                     linkService.getDeviceLinks(DID1).size());
        assertEquals("The link set size did not match.", 2,
                     linkService.getDeviceLinks(DID2).size());
        assertEquals("The link set size did not match.", 0,
                     linkService.getDeviceLinks(DID3).size());

        // test the getDeviceEgressLinks() method
        assertEquals("The link set size did not match.", 1,
                     linkService.getDeviceEgressLinks(DID1).size());
        assertEquals("The link set size did not match.", 1,
                     linkService.getDeviceEgressLinks(DID2).size());
        assertEquals("The link set size did not match.", 0,
                     linkService.getDeviceEgressLinks(DID3).size());

        // test the getDeviceIngressLinks() method
        assertEquals("The link set size did not match.", 1,
                     linkService.getDeviceIngressLinks(DID1).size());
        assertEquals("The link set size did not match.", 1,
                     linkService.getDeviceIngressLinks(DID2).size());
        assertEquals("The link set size did not match.", 0,
                     linkService.getDeviceIngressLinks(DID3).size());

        // test the getEgressLinks() method
        assertEquals("The link set size did not match.", 1,
                     linkService.getEgressLinks(src).size());
        assertEquals("The link set size did not match.", 1,
                     linkService.getEgressLinks(dst).size());
        assertEquals("The link set size did not match.", 0,
                     linkService.getEgressLinks(connectPoint).size());

        // test the getIngressLinks() method
        assertEquals("The link set size did not match.", 1,
                     linkService.getIngressLinks(src).size());
        assertEquals("The link set size did not match.", 1,
                     linkService.getIngressLinks(dst).size());
        assertEquals("The link set size did not match.", 0,
                     linkService.getIngressLinks(connectPoint).size());
    }

    /**
     * Tests the getLink() method using a null src connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testGetLinkByNullSrc() {

        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualLink(virtualNetwork.id(), src, dst);
        manager.createVirtualLink(virtualNetwork.id(), dst, src);

        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getLink() method with a null src connect point.
        linkService.getLink(null, dst);
    }

    /**
     * Tests the getLink() method using a null dst connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testGetLinkByNullDst() {

        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        VirtualDevice srcVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        VirtualDevice dstVirtualDevice =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        ConnectPoint src = new ConnectPoint(srcVirtualDevice.id(), PortNumber.portNumber(1));
        ConnectPoint dst = new ConnectPoint(dstVirtualDevice.id(), PortNumber.portNumber(2));
        manager.createVirtualLink(virtualNetwork.id(), src, dst);
        manager.createVirtualLink(virtualNetwork.id(), dst, src);

        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getLink() method with a null dst connect point.
        linkService.getLink(src, null);
    }

    /**
     * Tests querying for links using a null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDeviceLinksByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getDeviceLinks() method with a null device identifier.
        linkService.getDeviceLinks(null);
    }

    /**
     * Tests querying for links using a null connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testGetLinksByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getLinks() method with a null connect point.
        linkService.getLinks(null);
    }

    /**
     * Tests querying for device egress links using a null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDeviceEgressLinksByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getDeviceEgressLinks() method with a null device identifier.
        linkService.getDeviceEgressLinks(null);
    }

    /**
     * Tests querying for device ingress links using a null device identifier.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDeviceIngressLinksByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getDeviceIngressLinks() method with a null device identifier.
        linkService.getDeviceIngressLinks(null);
    }

    /**
     * Tests querying for egress links using a null connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testGetEgressLinksByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getEgressLinks() method with a null connect point.
        linkService.getEgressLinks(null);
    }

    /**
     * Tests querying for ingress links using a null connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testGetIngressLinksByNullId() {
        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        VirtualNetwork virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));
        LinkService linkService = manager.get(virtualNetwork.id(), LinkService.class);

        // test the getIngressLinks() method with a null connect point.
        linkService.getIngressLinks(null);
    }

    /**
     * Core service test class.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }
}
