package org.onlab.onos.net.trivial.topology.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.GraphDescription;
import org.onlab.onos.net.topology.TopologyProvider;
import org.onlab.onos.net.topology.TopologyProviderRegistry;
import org.onlab.onos.net.topology.TopologyProviderService;
import org.onlab.onos.net.trivial.device.impl.DeviceManager;
import org.onlab.onos.net.trivial.link.impl.SimpleLinkManager;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onlab.onos.net.trivial.topology.impl.SimpleTopologyManagerTest.device;
import static org.onlab.onos.net.trivial.topology.impl.SimpleTopologyManagerTest.link;

/**
 * Test of the default topology provider implementation.
 */
public class DefaultTopologyProviderTest {

    private DefaultTopologyProvider provider = new DefaultTopologyProvider();
    private TestTopoRegistry topologyService = new TestTopoRegistry();
    private TestDeviceService deviceService = new TestDeviceService();
    private TestLinkService linkService = new TestLinkService();
    private TestTopoProviderService providerService;

    @Before
    public void setUp() {
        provider.deviceService = deviceService;
        provider.linkService = linkService;
        provider.providerRegistry = topologyService;
        provider.activate();
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.providerRegistry = null;
        provider.deviceService = null;
        provider.linkService = null;
    }

    private void validateSubmission() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
        assertNotNull("topo change should be submitted", providerService.graphDesc);
        assertEquals("incorrect vertex count", 6, providerService.graphDesc.vertexes().size());
        assertEquals("incorrect edge count", 10, providerService.graphDesc.edges().size());
    }

    @Test
    public void basics() {
        assertAfter(100, new Runnable() {
            @Override
            public void run() {
                validateSubmission();
            }
        });
    }

    @Test
    public void eventDriven() {
        assertAfter(100, new Runnable() {
            @Override
            public void run() {
                validateSubmission();
                deviceService.post(new DeviceEvent(DEVICE_ADDED, device("z"), null));
                linkService.post(new LinkEvent(LINK_ADDED, link("z", 1, "a", 4)));
                validateSubmission();
            }
        });
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
        }
    }

    private class TestDeviceService extends DeviceManager {
        TestDeviceService() {
            eventDispatcher = new TestEventDispatcher();
            eventDispatcher.addSink(DeviceEvent.class, listenerRegistry);
        }

        @Override
        public Iterable<Device> getDevices() {
            return of(device("a"), device("b"),
                      device("c"), device("d"),
                      device("e"), device("f"));
        }

        void post(DeviceEvent event) {
            eventDispatcher.post(event);
        }
    }

    private class TestLinkService extends SimpleLinkManager {
        TestLinkService() {
            eventDispatcher = new TestEventDispatcher();
            eventDispatcher.addSink(LinkEvent.class, listenerRegistry);
        }

        @Override
        public Iterable<Link> getLinks() {
            return of(link("a", 1, "b", 1), link("b", 1, "a", 1),
                      link("b", 2, "c", 1), link("c", 1, "b", 2),
                      link("c", 2, "d", 1), link("d", 1, "c", 2),
                      link("d", 2, "a", 2), link("a", 2, "d", 2),
                      link("e", 1, "f", 1), link("f", 1, "e", 1));
        }

        void post(LinkEvent event) {
            eventDispatcher.post(event);
        }
    }
}