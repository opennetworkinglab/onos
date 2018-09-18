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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.ScalarWeight;
import org.onosproject.event.Event;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.DefaultGraphDescription;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.LinkWeigherAdapter;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyProvider;
import org.onosproject.net.topology.TopologyProviderRegistry;
import org.onosproject.net.topology.TopologyProviderService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.trivial.SimpleTopologyStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.*;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.topology.ClusterId.clusterId;
import static org.onosproject.net.topology.TopologyEvent.Type.TOPOLOGY_CHANGED;

/**
 * Test of the topology subsystem.
 */
public class TopologyManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");

    private TopologyManager mgr;

    protected TopologyService service;
    protected TopologyProviderRegistry registry;
    protected TopologyProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new TopologyManager();
        service = mgr;
        registry = mgr;

        mgr.store = new SimpleTopologyStore();
        injectEventDispatcher(mgr, new TestEventDispatcher());
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
        GraphDescription data = new DefaultGraphDescription(4321L, System.currentTimeMillis(), devices, links);
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
        assertEquals("wrong path cost", ScalarWeight.toWeight(2), path.weight());
    }

    @Test
    public void onDemandPath() {
        submitTopologyGraph();
        Topology topology = service.currentTopology();
        LinkWeigher weight = new LinkWeigherAdapter(3.3);

        Set<Path> paths = service.getPaths(topology, did("a"), did("c"), weight);
        assertEquals("wrong path count", 2, paths.size());
        Path path = paths.iterator().next();
        assertEquals("wrong path length", 2, path.links().size());
        assertEquals("wrong path cost", ScalarWeight.toWeight(6.6), path.weight());
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

        @Override
        public void triggerRecompute() {
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
