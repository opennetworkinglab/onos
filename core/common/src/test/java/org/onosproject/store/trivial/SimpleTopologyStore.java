/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.common.DefaultTopology;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyStore;
import org.onosproject.net.topology.TopologyStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of topology snapshots using trivial in-memory
 * structures implementation.
 */
@Component(immediate = true, service = TopologyStore.class)
public class SimpleTopologyStore
        extends AbstractStore<TopologyEvent, TopologyStoreDelegate>
        implements TopologyStore {

    private final Logger log = getLogger(getClass());

    private volatile DefaultTopology current;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }
    @Override
    public Topology currentTopology() {
        return current;
    }

    @Override
    public boolean isLatest(Topology topology) {
        // Topology is current only if it is the same as our current topology
        return topology == current;
    }

    @Override
    public TopologyGraph getGraph(Topology topology) {
        return defaultTopology(topology).getGraph();
    }

    @Override
    public Set<TopologyCluster> getClusters(Topology topology) {
        return defaultTopology(topology).getClusters();
    }

    @Override
    public TopologyCluster getCluster(Topology topology, ClusterId clusterId) {
        return defaultTopology(topology).getCluster(clusterId);
    }

    @Override
    public Set<DeviceId> getClusterDevices(Topology topology, TopologyCluster cluster) {
        return defaultTopology(topology).getClusterDevices(cluster);
    }

    @Override
    public Set<Link> getClusterLinks(Topology topology, TopologyCluster cluster) {
        return defaultTopology(topology).getClusterLinks(cluster);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        return defaultTopology(topology).getPaths(src, dst);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src,
                              DeviceId dst, LinkWeigher weigher) {
        return defaultTopology(topology).getPaths(src, dst, weigher);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst) {
        return defaultTopology(topology).getDisjointPaths(src, dst);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst, LinkWeigher weigher) {
        return defaultTopology(topology).getDisjointPaths(src, dst, weigher);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                                  Map<Link, Object> riskProfile) {
        return defaultTopology(topology).getDisjointPaths(src, dst, riskProfile);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              LinkWeigher weigher,
                                              Map<Link, Object> riskProfile) {
        return defaultTopology(topology).getDisjointPaths(src, dst, weigher, riskProfile);
    }

    @Override
    public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        return defaultTopology(topology).isInfrastructure(connectPoint);
    }

    @Override
    public boolean isBroadcastPoint(Topology topology, ConnectPoint connectPoint) {
        return defaultTopology(topology).isBroadcastPoint(connectPoint);
    }

    @Override
    public TopologyEvent updateTopology(ProviderId providerId,
                                        GraphDescription graphDescription,
                                        List<Event> reasons) {
        // First off, make sure that what we're given is indeed newer than
        // what we already have.
        if (current != null && graphDescription.timestamp() < current.time()) {
            return null;
        }

        // Have the default topology construct self from the description data.
        DefaultTopology newTopology =
                new DefaultTopology(providerId, graphDescription);

        // Promote the new topology to current and return a ready-to-send event.
        synchronized (this) {
            current = newTopology;
            return new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGED,
                                     current, reasons);
        }
    }

    // Validates the specified topology and returns it as a default
    private DefaultTopology defaultTopology(Topology topology) {
        if (topology instanceof DefaultTopology) {
            return (DefaultTopology) topology;
        }
        throw new IllegalArgumentException("Topology class " + topology.getClass() +
                                                   " not supported");
    }

}
