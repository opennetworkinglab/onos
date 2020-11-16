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
package org.onosproject.store.topology.impl;

import org.onlab.graph.GraphPathSearch;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.common.DefaultTopology;
import org.onosproject.event.Event;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.DefaultGraphDescription;
import org.onosproject.net.topology.GeoDistanceLinkWeight;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.MetricLinkWeight;
import org.onosproject.net.topology.PathAdminService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyStore;
import org.onosproject.net.topology.TopologyStoreDelegate;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.isNullOrEmpty;
import static org.onosproject.net.topology.TopologyEvent.Type.TOPOLOGY_CHANGED;
import static org.onosproject.store.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of topology snapshots using trivial in-memory
 * structures implementation.
 * <p>
 * Note: This component is not distributed per-se. It runs on every
 * instance and feeds off of other distributed stores.
 */
@Component(
        immediate = true,
        service = {
                TopologyStore.class, PathAdminService.class
        },
        property = {
                LINK_WEIGHT_FUNCTION + "=" + LINK_WEIGHT_FUNCTION_DEFAULT,
                MAX_PATHS + "=" + MAX_PATHS_DEFAULT,
        }
)
public class DistributedTopologyStore
        extends AbstractStore<TopologyEvent, TopologyStoreDelegate>
        implements TopologyStore, PathAdminService {

    private final Logger log = getLogger(getClass());

    private static final String FORMAT = "Settings: linkWeightFunction={}";

    private volatile DefaultTopology current =
            new DefaultTopology(ProviderId.NONE,
                                new DefaultGraphDescription(0L, System.currentTimeMillis(),
                                                            Collections.emptyList(),
                                                            Collections.emptyList()));

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    private static final String HOP_COUNT = "hopCount";
    private static final String LINK_METRIC = "linkMetric";
    private static final String GEO_DISTANCE = "geoDistance";

    /** Default link-weight function: hopCount, linkMetric, geoDistance. */
    private String linkWeightFunction = LINK_WEIGHT_FUNCTION_DEFAULT;

    /** Default max-paths count. */
    private int maxPaths = ALL_PATHS;

    // Cluster root to broadcast points bindings to allow convergence to
    // a shared broadcast tree; node that is the master of the cluster root
    // is the primary.
    private EventuallyConsistentMap<DeviceId, Set<ConnectPoint>> broadcastPoints;

    private EventuallyConsistentMapListener<DeviceId, Set<ConnectPoint>> listener =
            new InternalBroadcastPointListener();

    @Activate
    protected void activate(ComponentContext context) {
        configService.registerProperties(getClass());
        modified(context);
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
    protected void deactivate() {
        configService.unregisterProperties(getClass(), false);
        broadcastPoints.removeListener(listener);
        broadcastPoints.destroy();
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String newLinkWeightFunction = get(properties, LINK_WEIGHT_FUNCTION);
        if (newLinkWeightFunction != null &&
                !Objects.equals(newLinkWeightFunction, linkWeightFunction)) {
            linkWeightFunction = newLinkWeightFunction;
            LinkWeigher weight = linkWeightFunction.equals(LINK_METRIC) ?
                    new MetricLinkWeight() :
                    linkWeightFunction.equals(GEO_DISTANCE) ?
                            new GeoDistanceLinkWeight(deviceService) : null;
            setDefaultLinkWeigher(weight);
        }

        String newMaxPaths = get(properties, MAX_PATHS);
        if (newMaxPaths != null) {
            try {
                setDefaultMaxPaths(Integer.parseInt(newMaxPaths));
            } catch (NumberFormatException e) {
                log.warn("maxPaths must be a number; not {}", newMaxPaths);
            }
        }
        log.info(FORMAT, linkWeightFunction);
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
    public Set<Path> getKShortestPaths(Topology topology,
                                       DeviceId src, DeviceId dst,
                                       LinkWeigher weigher,
                                       int maxPaths) {
        return defaultTopology(topology).getKShortestPaths(src, dst, weigher, maxPaths);
    }

    @Override
    public Stream<Path> getKShortestPaths(Topology topology,
                                          DeviceId src,
                                          DeviceId dst,
                                          LinkWeigher weigher) {
        return defaultTopology(topology).getKShortestPaths(src, dst, weigher);
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
                                              DeviceId dst, LinkWeigher weigher,
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
        // Have the default topology construct self from the description data.
        DefaultTopology newTopology =
                new DefaultTopology(providerId, graphDescription, this::isBroadcastPoint);
        updateBroadcastPoints(newTopology);

        // Promote the new topology to current and return a ready-to-send event.
        synchronized (this) {
            // Make sure that what we're given is indeed newer than what we
            // already have.
            if (current != null && newTopology.time() < current.time()) {
                return null;
            }
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

    @Override
    public void setDefaultMaxPaths(int maxPaths) {
        this.maxPaths = maxPaths;
        DefaultTopology.setDefaultMaxPaths(maxPaths);
    }
    @Override
    public void setDefaultLinkWeigher(LinkWeigher linkWeigher) {
        DefaultTopology.setDefaultLinkWeigher(linkWeigher);
    }

    @Override
    public void setDefaultGraphPathSearch(GraphPathSearch<TopologyVertex, TopologyEdge> graphPathSearch) {
        DefaultTopology.setDefaultGraphPathSearch(graphPathSearch);
    }

    private class InternalBroadcastPointListener
            implements EventuallyConsistentMapListener<DeviceId, Set<ConnectPoint>> {
        @Override
        public void event(EventuallyConsistentMapEvent<DeviceId, Set<ConnectPoint>> event) {
            if (event.type() == EventuallyConsistentMapEvent.Type.PUT) {
                if (!event.value().isEmpty()) {
                    log.debug("Cluster rooted at {} has {} broadcast-points; #{}",
                             event.key(), event.value().size(), event.value().hashCode());
                }
            }
        }
    }
}
