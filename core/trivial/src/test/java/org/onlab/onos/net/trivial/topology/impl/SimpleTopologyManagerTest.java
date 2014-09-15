package org.onlab.onos.net.trivial.topology.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.GraphDescription;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyEdge;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyGraph;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyProvider;
import org.onlab.onos.net.topology.TopologyProviderRegistry;
import org.onlab.onos.net.topology.TopologyProviderService;
import org.onlab.onos.net.topology.TopologyService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.*;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;
import static org.onlab.onos.net.topology.ClusterId.clusterId;
import static org.onlab.onos.net.topology.TopologyEvent.Type.TOPOLOGY_CHANGED;

/**
 * Test of the topology subsystem.
 */
public class SimpleTopologyManagerTest {

    private static final ProviderId PID = new ProviderId("foo");

    private SimpleTopologyManager mgr;

    protected TopologyService service;
    protected TopologyProviderRegistry registry;
    protected TopologyProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new SimpleTopologyManager();
        service = mgr;
        registry = mgr;

        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.activate();

        service.addListener(listener);

        provider = new TestProvider();
        providerService = registry.register(provider);

        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        mgr.deactivate();
        service.removeListener(listener);
    }

    @Test
    public void basics() {
        Topology topology = service.currentTopology();
        assertNull("no topo expected", topology);
        submitTopologyGraph();
        validateEvents(TOPOLOGY_CHANGED);
        topology = service.currentTopology();
        assertTrue("should be latest", service.isLatest(topology));

        submitTopologyGraph();
        validateEvents(TOPOLOGY_CHANGED);
        assertFalse("should be latest", service.isLatest(topology));
    }

    private void submitTopologyGraph() {
        Set<Device> devices = of(device("a"), device("b"),
                                 device("c"), device("d"),
                                 device("e"), device("f"));
        Set<Link> links = of(link("a", 1, "b", 1), link("b", 1, "a", 1),
                             link("b", 2, "c", 1), link("c", 1, "b", 2),
                             link("c", 2, "d", 1), link("d", 1, "c", 2),
                             link("d", 2, "a", 2), link("a", 2, "d", 2),
                             link("e", 1, "f", 1), link("f", 1, "e", 1));
        GraphDescription data = new DefaultGraphDescription(4321L, devices, links);
        providerService.topologyChanged(data, null);
    }

    @Test
    public void clusters() {
        submitTopologyGraph();
        Topology topology = service.currentTopology();
        assertNotNull("topo expected", topology);
        assertEquals("wrong cluster count", 2, topology.clusterCount());
        assertEquals("wrong device count", 6, topology.deviceCount());
        assertEquals("wrong link count", 10, topology.linkCount());
        assertEquals("wrong path count", 18, topology.pathCount());

        assertEquals("wrong cluster count", 2, service.getClusters(topology).size());

        TopologyCluster cluster = service.getCluster(topology, clusterId(0));
        assertEquals("wrong device count", 4, cluster.deviceCount());
        assertEquals("wrong device count", 4, service.getClusterDevices(topology, cluster).size());
        assertEquals("wrong link count", 8, cluster.linkCount());
        assertEquals("wrong link count", 8, service.getClusterLinks(topology, cluster).size());
    }

    @Test
    public void structure() {
        submitTopologyGraph();
        Topology topology = service.currentTopology();

        assertTrue("should be infrastructure point",
                   service.isInfrastructure(topology, new ConnectPoint(did("a"), portNumber(1))));
        assertFalse("should not be infrastructure point",
                    service.isInfrastructure(topology, new ConnectPoint(did("a"), portNumber(3))));

        // One of these cannot be a broadcast point... or we have a loop...
        assertFalse("should not be broadcast point",
                    service.isBroadcastPoint(topology, new ConnectPoint(did("a"), portNumber(1))) &&
                            service.isBroadcastPoint(topology, new ConnectPoint(did("b"), portNumber(1))) &&
                            service.isBroadcastPoint(topology, new ConnectPoint(did("c"), portNumber(1))) &&
                            service.isBroadcastPoint(topology, new ConnectPoint(did("d"), portNumber(1))));
        assertTrue("should be broadcast point",
                   service.isBroadcastPoint(topology, new ConnectPoint(did("a"), portNumber(3))));
    }

    @Test
    public void graph() {
        submitTopologyGraph();
        Topology topology = service.currentTopology();
        TopologyGraph graph = service.getGraph(topology);
        assertEquals("wrong vertex count", 6, graph.getVertexes().size());
        assertEquals("wrong edge count", 10, graph.getEdges().size());
    }

    @Test
    public void precomputedPath() {
        submitTopologyGraph();
        Topology topology = service.currentTopology();
        Set<Path> paths = service.getPaths(topology, did("a"), did("c"));
        assertEquals("wrong path count", 2, paths.size());
        Path path = paths.iterator().next();
        assertEquals("wrong path length", 2, path.links().size());
        assertEquals("wrong path cost", 2, path.cost(), 0.01);
    }

    @Test
    public void onDemandPath() {
        submitTopologyGraph();
        Topology topology = service.currentTopology();
        LinkWeight weight = new LinkWeight() {
            @Override
            public double weight(TopologyEdge edge) {
                return 3.3;
            }
        };

        Set<Path> paths = service.getPaths(topology, did("a"), did("c"), weight);
        assertEquals("wrong path count", 2, paths.size());
        Path path = paths.iterator().next();
        assertEquals("wrong path length", 2, path.links().size());
        assertEquals("wrong path cost", 6.6, path.cost(), 0.01);
    }

    // Short-hand for creating a link.
    private Link link(String src, int sp, String dst, int dp) {
        return new DefaultLink(PID, new ConnectPoint(did(src), portNumber(sp)),
                               new ConnectPoint(did(dst), portNumber(dp)),
                               Link.Type.DIRECT);
    }

    // Crates a new device with the specified id
    private Device device(String id) {
        return new DefaultDevice(PID, did(id), Device.Type.SWITCH,
                                 "mfg", "1.0", "1.1", "1234");
    }

    // Short-hand for producing a device id from a string
    private DeviceId did(String id) {
        return deviceId("of:" + id);
    }

    protected void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("wrong events received", types.length, listener.events.size());
        for (Event event : listener.events) {
            assertEquals("incorrect event type", types[i], event.type());
            i++;
        }
        listener.events.clear();
    }

    private class TestProvider extends AbstractProvider implements TopologyProvider {
        public TestProvider() {
            super(PID);
        }
    }

    private static class TestListener implements TopologyListener {
        final List<TopologyEvent> events = new ArrayList<>();

        @Override
        public void event(TopologyEvent event) {
            events.add(event);
        }
    }

}