/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.topology.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.isNullOrEmpty;
import static org.onosproject.net.topology.TopologyEvent.Type.TOPOLOGY_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.common.DefaultTopology;
import org.onosproject.event.Event;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.DefaultGraphDescription;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyStore;
import org.onosproject.net.topology.TopologyStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

/**
 * Manages inventory of topology snapshots using trivial in-memory
 * structures implementation.
 * <p>
 * Note: This component is not distributed per-se. It runs on every
 * instance and feeds off of other distributed stores.
 */
@Component(immediate = true)
@Service
public class DistributedTopologyStore
        extends AbstractStore<TopologyEvent, TopologyStoreDelegate>
        implements TopologyStore {

    private final Logger log = getLogger(getClass());
    private volatile DefaultTopology current =
            new DefaultTopology(ProviderId.NONE,
                                new DefaultGraphDescription(0L, System.currentTimeMillis(),
                                                            Collections.<Device>emptyList(),
                                                            Collections.<Link>emptyList()));

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    // Cluster root to broadcast points bindings to allow convergence to
    // a shared broadcast tree; node that is the master of the cluster root
    // is the primary.
    private EventuallyConsistentMap<DeviceId, Set<ConnectPoint>> broadcastPoints;

    private EventuallyConsistentMapListener<DeviceId, Set<ConnectPoint>> listener =
            new InternalBroadcastPointListener();

    @Activate
    public void activate() {
        KryoNamespace.Builder hostSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API);

        broadcastPoints = storageService.<DeviceId, Set<ConnectPoint>>eventuallyConsistentMapBuilder()
                .withName("onos-broadcast-trees")
                .withSerializer(hostSerializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();
        broadcastPoints.addListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        broadcastPoints.removeListener(listener);
        broadcastPoints.destroy();
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
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst,
                              LinkWeight weight) {
        return defaultTopology(topology).getPaths(src, dst, weight);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst) {
        return defaultTopology(topology).getDisjointPaths(src, dst);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                              LinkWeight weight) {
        return defaultTopology(topology).getDisjointPaths(src, dst, weight);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                              Map<Link, Object> riskProfile) {
        return defaultTopology(topology).getDisjointPaths(src, dst, riskProfile);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                              LinkWeight weight, Map<Link, Object> riskProfile) {
        return defaultTopology(topology).getDisjointPaths(src, dst, weight, riskProfile);
    }

    @Override
    public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        return defaultTopology(topology).isInfrastructure(connectPoint);
    }

    @Override
    public boolean isBroadcastPoint(Topology topology, ConnectPoint connectPoint) {
        return defaultTopology(topology).isBroadcastPoint(connectPoint);
    }

    private boolean isBroadcastPoint(ConnectPoint connectPoint) {
        // Any non-infrastructure, i.e. edge points are assumed to be OK.
        if (!current.isInfrastructure(connectPoint)) {
            return true;
        }

        // Find the cluster to which the device belongs.
        TopologyCluster cluster = current.getCluster(connectPoint.deviceId());
        checkArgument(cluster != null, "No cluster found for device %s", connectPoint.deviceId());

        // If the broadcast set is null or empty, or if the point explicitly
        // belongs to it, return true;
        Set<ConnectPoint> points = broadcastPoints.get(cluster.root().deviceId());
        return isNullOrEmpty(points) || points.contains(connectPoint);
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
                new DefaultTopology(providerId, graphDescription, this::isBroadcastPoint);
        updateBroadcastPoints(newTopology);

        // Promote the new topology to current and return a ready-to-send event.
        synchronized (this) {
            current = newTopology;
            return new TopologyEvent(TOPOLOGY_CHANGED, current, reasons);
        }
    }

    private void updateBroadcastPoints(DefaultTopology topology) {
        // Remove any broadcast trees rooted by devices for which we are master.
        Set<DeviceId> toRemove = broadcastPoints.keySet().stream()
                .filter(mastershipService::isLocalMaster)
                .collect(Collectors.toSet());

        // Update the broadcast trees rooted by devices for which we are master.
        topology.getClusters().forEach(c -> {
            toRemove.remove(c.root().deviceId());
            if (mastershipService.isLocalMaster(c.root().deviceId())) {
                broadcastPoints.put(c.root().deviceId(),
                                    topology.broadcastPoints(c.id()));
            }
        });

        toRemove.forEach(broadcastPoints::remove);
    }

    // Validates the specified topology and returns it as a default
    private DefaultTopology defaultTopology(Topology topology) {
        checkArgument(topology instanceof DefaultTopology,
                      "Topology class %s not supported", topology.getClass());
        return (DefaultTopology) topology;
    }

    private class InternalBroadcastPointListener
            implements EventuallyConsistentMapListener<DeviceId, Set<ConnectPoint>> {
        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId, Set<ConnectPoint>> event) {
            if (event.type() == EventuallyConsistentMapEvent.Type.PUT) {
                if (!event.value().isEmpty()) {
                    log.info("Cluster rooted at {} has {} broadcast-points; #{}",
                             event.key(), event.value().size(), event.value().hashCode());
                }
            }
        }
    }
}
