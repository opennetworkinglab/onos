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

package org.onosproject.incubator.net.virtual.impl.provider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.Event;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProviderRegistry;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProviderService;
import org.onosproject.incubator.net.virtual.impl.VirtualNetworkManager;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TestDeviceParams;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.TestStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Junit tests for VirtualNetworkTopologyProvider.
 */
public class VirtualNetworkTopologyProviderTest extends TestDeviceParams {

    private final String tenantIdValue1 = "TENANT_ID1";

    private VirtualNetwork virtualNetwork;
    private VirtualDevice virtualDevice1;
    private VirtualDevice virtualDevice2;
    private VirtualDevice virtualDevice3;
    private VirtualDevice virtualDevice4;
    private VirtualDevice virtualDevice5;
    private ConnectPoint cp1;
    private ConnectPoint cp2;
    private ConnectPoint cp3;
    private ConnectPoint cp4;
    private ConnectPoint cp5;
    private ConnectPoint cp6;
    private ConnectPoint cp7;
    private ConnectPoint cp8;
    private ConnectPoint cp9;

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService;
    private DefaultVirtualNetworkProvider topologyProvider;
    private TopologyService topologyService;
    private TestableIntentService intentService = new FakeIntentManager();
    private TestServiceDirectory testDirectory;
    private final VirtualNetworkRegistryAdapter virtualNetworkRegistry = new VirtualNetworkRegistryAdapter();

    private static final int MAX_WAIT_TIME = 5;
    private static final int MAX_PERMITS = 1;
    private static Semaphore changed;

    private Set<Set<ConnectPoint>> clusters;

    @Before
    public void setUp() throws Exception {

        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        coreService = new VirtualNetworkTopologyProviderTest.TestCoreService();

        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService",
                           new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        TestUtils.setField(manager, "coreService", coreService);
        TestUtils.setField(manager, "store", virtualNetworkManagerStore);
        TestUtils.setField(manager, "intentService", intentService);
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());

        testDirectory = new TestServiceDirectory();
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();

        manager.registerTenantId(TenantId.tenantId(tenantIdValue1));
        virtualNetwork = manager.createVirtualNetwork(TenantId.tenantId(tenantIdValue1));

        topologyService = manager.get(virtualNetwork.id(), TopologyService.class);
        topologyProvider = new DefaultVirtualNetworkProvider();
        topologyProvider.topologyService = topologyService;
        topologyProvider.providerRegistry = virtualNetworkRegistry;
        topologyProvider.activate();

        setupVirtualNetworkTopology();
        changed = new Semaphore(0, true);
    }

    @After
    public void tearDown() {
        topologyProvider.deactivate();
        virtualNetworkManagerStore.deactivate();
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
    }

    /**
     * Method to create the virtual network for further testing.
     **/
    private void setupVirtualNetworkTopology() {
        virtualDevice1 =
                manager.createVirtualDevice(virtualNetwork.id(), DID1);
        virtualDevice2 =
                manager.createVirtualDevice(virtualNetwork.id(), DID2);
        virtualDevice3 =
                manager.createVirtualDevice(virtualNetwork.id(), DID3);
        virtualDevice4 =
                manager.createVirtualDevice(virtualNetwork.id(), DID4);
        virtualDevice5 =
                manager.createVirtualDevice(virtualNetwork.id(), DID5);

        cp1 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(),
                                  PortNumber.portNumber(1), cp1);

        cp2 = new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(2));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice1.id(),
                                  PortNumber.portNumber(2), cp2);

        cp3 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(3));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(),
                                  PortNumber.portNumber(3), cp3);

        cp4 = new ConnectPoint(virtualDevice2.id(), PortNumber.portNumber(4));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice2.id(),
                                  PortNumber.portNumber(4), cp4);

        cp5 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(5));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(),
                                  PortNumber.portNumber(5), cp5);

        cp6 = new ConnectPoint(virtualDevice3.id(), PortNumber.portNumber(6));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice3.id(),
                                  PortNumber.portNumber(6), cp6);

        cp7 = new ConnectPoint(virtualDevice4.id(), PortNumber.portNumber(7));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice4.id(),
                                  PortNumber.portNumber(7), cp7);

        cp8 = new ConnectPoint(virtualDevice4.id(), PortNumber.portNumber(8));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice4.id(),
                                  PortNumber.portNumber(8), cp8);

        cp9 = new ConnectPoint(virtualDevice5.id(), PortNumber.portNumber(9));
        manager.createVirtualPort(virtualNetwork.id(), virtualDevice5.id(),
                                  PortNumber.portNumber(9), cp9);

        VirtualLink link1 = manager.createVirtualLink(virtualNetwork.id(), cp1, cp3);
        virtualNetworkManagerStore.updateLink(link1, link1.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), cp3, cp1);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);
        VirtualLink link3 = manager.createVirtualLink(virtualNetwork.id(), cp4, cp5);
        virtualNetworkManagerStore.updateLink(link3, link3.tunnelId(), Link.State.ACTIVE);
        VirtualLink link4 = manager.createVirtualLink(virtualNetwork.id(), cp5, cp4);
        virtualNetworkManagerStore.updateLink(link4, link4.tunnelId(), Link.State.ACTIVE);
        VirtualLink link5 = manager.createVirtualLink(virtualNetwork.id(), cp8, cp9);
        virtualNetworkManagerStore.updateLink(link5, link5.tunnelId(), Link.State.ACTIVE);
        VirtualLink link6 = manager.createVirtualLink(virtualNetwork.id(), cp9, cp8);
        virtualNetworkManagerStore.updateLink(link6, link6.tunnelId(), Link.State.ACTIVE);

        clusters = null;
    }

    /**
     * Test isTraversable() method using a null source connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testIsTraversableNullSrc() {
        // test the isTraversable() method with a null source connect point.
        topologyProvider.isTraversable(null, cp3);
    }

    /**
     * Test isTraversable() method using a null destination connect point.
     */
    @Test(expected = NullPointerException.class)
    public void testIsTraversableNullDst() {
        // test the isTraversable() method with a null destination connect point.
        topologyProvider.isTraversable(cp1, null);
    }

    /**
     * Test isTraversable() method.
     */
    @Test
    public void testIsTraversable() {
        // test the isTraversable() method.
        assertTrue("These two connect points should be traversable.",
                   topologyProvider.isTraversable(new ConnectPoint(cp1.elementId(), cp1.port()),
                                                  new ConnectPoint(cp3.elementId(), cp3.port())));
        assertTrue("These two connect points should be traversable.",
                   topologyProvider.isTraversable(new ConnectPoint(cp1.elementId(), cp1.port()),
                                                  new ConnectPoint(cp5.elementId(), cp5.port())));
        assertFalse("These two connect points should not be traversable.",
                    topologyProvider.isTraversable(
                            new ConnectPoint(virtualDevice1.id(), PortNumber.portNumber(1)),
                            new ConnectPoint(virtualDevice4.id(), PortNumber.portNumber(6))));
    }

    /**
     * Test the topologyChanged() method.
     */
    @Test
    public void testTopologyChanged() {
        // Initial setup is two clusters of devices/links.
        assertEquals("The cluster count did not match.", 2,
                     topologyService.currentTopology().clusterCount());

        // Adding this link will join the two clusters together.
        List<Event> reasons = new ArrayList<>();
        VirtualLink link = manager.createVirtualLink(virtualNetwork.id(), cp6, cp7);
        virtualNetworkManagerStore.updateLink(link, link.tunnelId(), Link.State.ACTIVE);
        VirtualLink link2 = manager.createVirtualLink(virtualNetwork.id(), cp7, cp6);
        virtualNetworkManagerStore.updateLink(link2, link2.tunnelId(), Link.State.ACTIVE);

        reasons.add(new LinkEvent(LinkEvent.Type.LINK_ADDED, link));
        reasons.add(new LinkEvent(LinkEvent.Type.LINK_ADDED, link2));
        TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topologyService.currentTopology(),
                reasons);

        topologyProvider.topologyListener.event(event);

        // Wait for the topology changed event, and that the topologyChanged method was called.
        try {
            if (!changed.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for topology changed event.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception." + e.getMessage());
        }

        // Validate that the topology changed method received a single cluster of connect points.
        // This means that the two previous clusters have now joined into a single cluster.
        assertEquals("The cluster count did not match.", 1, this.clusters.size());
        assertEquals("The cluster count did not match.", 1,
                     topologyService.currentTopology().clusterCount());

        // Now remove the virtual link to split it back into two clusters.
        manager.removeVirtualLink(virtualNetwork.id(), link.src(), link.dst());
        manager.removeVirtualLink(virtualNetwork.id(), link2.src(), link2.dst());
        assertEquals("The cluster count did not match.", 2,
                     topologyService.currentTopology().clusterCount());

        reasons = new ArrayList<>();
        reasons.add(new LinkEvent(LinkEvent.Type.LINK_REMOVED, link));
        reasons.add(new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2));
        event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topologyService.currentTopology(),
                reasons);

        topologyProvider.topologyListener.event(event);

        // Wait for the topology changed event, and that the topologyChanged method was called.
        try {
            if (!changed.tryAcquire(MAX_PERMITS, MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                fail("Failed to wait for topology changed event.");
            }
        } catch (InterruptedException e) {
            fail("Semaphore exception." + e.getMessage());
        }

        // Validate that the topology changed method received two clusters of connect points.
        // This means that the single previous clusters has now split into two clusters.
        assertEquals("The cluster count did not match.", 2, this.clusters.size());
    }

    /**
     * Virtual network registry implementation for this test class.
     */
    private class VirtualNetworkRegistryAdapter implements VirtualNetworkProviderRegistry {
        private VirtualNetworkProvider provider;

        @Override
        public VirtualNetworkProviderService register(VirtualNetworkProvider theProvider) {
            this.provider = theProvider;
            return new TestVirtualNetworkProviderService(theProvider);
        }

        @Override
        public void unregister(VirtualNetworkProvider theProvider) {
            this.provider = null;
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }
    }


    /**
     * Virtual network provider service implementation for this test class.
     */
    private class TestVirtualNetworkProviderService
            extends AbstractProviderService<VirtualNetworkProvider>
            implements VirtualNetworkProviderService {

        /**
         * Constructor.
         *
         * @param provider virtual network test provider
         */
        protected TestVirtualNetworkProviderService(VirtualNetworkProvider provider) {
            super(provider);
        }

        @Override
        public void topologyChanged(Set<Set<ConnectPoint>> theClusters) {
            clusters = theClusters;
            changed.release();
        }

        @Override
        public void tunnelUp(NetworkId networkId, ConnectPoint src,
                             ConnectPoint dst, TunnelId tunnelId) {
        }

        @Override
        public void tunnelDown(NetworkId networkId, ConnectPoint src,
                               ConnectPoint dst, TunnelId tunnelId) {
        }
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
