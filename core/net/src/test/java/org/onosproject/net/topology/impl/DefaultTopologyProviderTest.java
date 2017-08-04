/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.topology.impl;

import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.event.Event;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.impl.DeviceManager;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.impl.LinkManager;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.TopologyProvider;
import org.onosproject.net.topology.TopologyProviderRegistry;
import org.onosproject.net.topology.TopologyProviderService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.onosproject.net.NetTestTools.device;
import static org.onosproject.net.NetTestTools.link;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;

/**
 * Test of the default topology provider implementation.
 */
public class DefaultTopologyProviderTest {

    private DefaultTopologyProvider provider = new DefaultTopologyProvider();
    private TestTopoRegistry topologyService = new TestTopoRegistry();
    private TestDeviceService deviceService = new TestDeviceService();
    private TestLinkService linkService = new TestLinkService();
    private TestTopoProviderService providerService;

    // phase corresponds to number of topologyChanged called
    private Phaser topologyChangedCounts = new Phaser(1);

    @Before
    public void setUp() {
        provider.deviceService = deviceService;
        provider.linkService = linkService;
        provider.providerRegistry = topologyService;
        provider.cfgService = new ComponentConfigAdapter();
        provider.activate(null);
    }

    @After
    public void tearDown() {
        provider.deactivate(null);
        provider.providerRegistry = null;
        provider.deviceService = null;
        provider.linkService = null;
        provider.cfgService = null;
    }

    private void validateSubmission() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
        assertNotNull("topo change should be submitted", providerService.graphDesc);
        assertEquals("incorrect vertex count", 6, providerService.graphDesc.vertexes().size());
        assertEquals("incorrect edge count", 10, providerService.graphDesc.edges().size());
    }

    @Test
    public void basics() throws InterruptedException, TimeoutException {
        assertEquals(1, topologyChangedCounts.awaitAdvanceInterruptibly(0, 1, TimeUnit.SECONDS));
        validateSubmission();
    }

    @Test
    public void eventDriven() throws InterruptedException, TimeoutException {
        assertEquals(1, topologyChangedCounts.awaitAdvanceInterruptibly(0, 1, TimeUnit.SECONDS));
        validateSubmission();

        deviceService.postEvent(new DeviceEvent(DEVICE_ADDED, device("z"), null));
        linkService.postEvent(new LinkEvent(LINK_ADDED, link("z", 1, "a", 4)));
        assertThat(topologyChangedCounts.awaitAdvanceInterruptibly(1, 1, TimeUnit.SECONDS),
                is(greaterThanOrEqualTo(2)));
        // Note: posting event, to trigger topologyChanged call,
        // but dummy topology will not change.
        validateSubmission();
    }


    private class TestTopoRegistry implements TopologyProviderRegistry {

        @Override
        public TopologyProviderService register(TopologyProvider provider) {
            providerService = new TestTopoProviderService(provider);
            return providerService;
        }

        @Override
        public void unregister(TopologyProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }
    }

    private class TestTopoProviderService
            extends AbstractProviderService<TopologyProvider>
            implements TopologyProviderService {
        GraphDescription graphDesc;

        protected TestTopoProviderService(TopologyProvider provider) {
            super(provider);
        }

        @Override
        public void topologyChanged(GraphDescription graphDescription, List<Event> reasons) {
            graphDesc = graphDescription;
            topologyChangedCounts.arrive();
        }
    }

    private class TestDeviceService extends DeviceManager {
        TestDeviceService() {
            eventDispatcher = new TestEventDispatcher();
            eventDispatcher.addSink(DeviceEvent.class, listenerRegistry);
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableSet.of(device("a"), device("b"),
                                   device("c"), device("d"),
                                   device("e"), device("f"));
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return getDevices();
        }

        void postEvent(DeviceEvent event) {
            eventDispatcher.post(event);
        }
    }

    private class TestLinkService extends LinkManager {
        TestLinkService() {
            eventDispatcher = new TestEventDispatcher();
            eventDispatcher.addSink(LinkEvent.class, listenerRegistry);
        }

        @Override
        public Iterable<Link> getLinks() {
            return ImmutableSet.of(link("a", 1, "b", 1), link("b", 1, "a", 1),
                                   link("b", 2, "c", 1), link("c", 1, "b", 2),
                                   link("c", 2, "d", 1), link("d", 1, "c", 2),
                                   link("d", 2, "a", 2), link("a", 2, "d", 2),
                                   link("e", 1, "f", 1), link("f", 1, "e", 1));
        }

        @Override
        public Iterable<Link> getActiveLinks() {
            return getLinks();
        }

        void postEvent(LinkEvent event) {
            eventDispatcher.post(event);
        }
    }
}
