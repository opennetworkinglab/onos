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

import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyProvider;
import org.onosproject.net.topology.TopologyProviderRegistry;
import org.onosproject.net.topology.TopologyProviderService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyStore;
import org.onosproject.net.topology.TopologyStoreDelegate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.TOPOLOGY_READ;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Provides basic implementation of the topology SB &amp; NB APIs.
 */
@Component(immediate = true, service = {TopologyService.class, TopologyProviderRegistry.class})
public class TopologyManager
        extends AbstractListenerProviderRegistry<TopologyEvent, TopologyListener,
        TopologyProvider, TopologyProviderService>
        implements TopologyService, TopologyProviderRegistry {

    private static final String TOPOLOGY_NULL = "Topology cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String CLUSTER_ID_NULL = "Cluster ID cannot be null";
    private static final String CLUSTER_NULL = "Topology cluster cannot be null";
    private static final String CONNECTION_POINT_NULL = "Connection point cannot be null";
    private static final String LINK_WEIGHT_NULL = "Link weight cannot be null";

    private final Logger log = getLogger(getClass());

    private TopologyStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyStore store;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(TopologyEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(TopologyEvent.class);
        log.info("Stopped");
    }

    @Override
    public Topology currentTopology() {
        checkPermission(TOPOLOGY_READ);
        return store.currentTopology();
    }

    @Override
    public boolean isLatest(Topology topology) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.isLatest(topology);
    }

    @Override
    public Set<TopologyCluster> getClusters(Topology topology) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.getClusters(topology);
    }

    @Override
    public TopologyCluster getCluster(Topology topology, ClusterId clusterId) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(clusterId, CLUSTER_ID_NULL);
        return store.getCluster(topology, clusterId);
    }

    @Override
    public Set<DeviceId> getClusterDevices(Topology topology, TopologyCluster cluster) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(cluster, CLUSTER_NULL);
        return store.getClusterDevices(topology, cluster);
    }

    @Override
    public Set<Link> getClusterLinks(Topology topology, TopologyCluster cluster) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(cluster, CLUSTER_NULL);
        return store.getClusterLinks(topology, cluster);
    }

    @Override
    public TopologyGraph getGraph(Topology topology) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.getGraph(topology);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        return store.getPaths(topology, src, dst);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src,
                              DeviceId dst, LinkWeigher weigher) {
        checkPermission(TOPOLOGY_READ);

        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        checkNotNull(weigher, LINK_WEIGHT_NULL);
        return store.getPaths(topology, src, dst, weigher);
    }

    @Override
    public Set<Path> getKShortestPaths(Topology topology, DeviceId src,
                                       DeviceId dst, LinkWeigher weigher,
                                       int maxPaths) {
        checkPermission(TOPOLOGY_READ);

        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        checkNotNull(weigher, LINK_WEIGHT_NULL);
        return store.getKShortestPaths(topology, src, dst, weigher, maxPaths);
    }

    @Override
    public Stream<Path> getKShortestPaths(Topology topology,
                                          DeviceId src,
                                          DeviceId dst,
                                          LinkWeigher weigher) {
        checkPermission(TOPOLOGY_READ);

        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        checkNotNull(weigher, LINK_WEIGHT_NULL);
        return store.getKShortestPaths(topology, src, dst, weigher);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        return store.getDisjointPaths(topology, src, dst);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              LinkWeigher weigher) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        checkNotNull(weigher, LINK_WEIGHT_NULL);
        return store.getDisjointPaths(topology, src, dst, weigher);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              Map<Link, Object> riskProfile) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        return store.getDisjointPaths(topology, src, dst, riskProfile);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              LinkWeigher weigher,
                                              Map<Link, Object> riskProfile) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        checkNotNull(weigher, LINK_WEIGHT_NULL);
        return store.getDisjointPaths(topology, src, dst, weigher, riskProfile);
    }

    @Override
    public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(connectPoint, CONNECTION_POINT_NULL);
        return store.isInfrastructure(topology, connectPoint);
    }

    @Override
    public boolean isBroadcastPoint(Topology topology, ConnectPoint connectPoint) {
        checkPermission(TOPOLOGY_READ);
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(connectPoint, CONNECTION_POINT_NULL);
        return store.isBroadcastPoint(topology, connectPoint);
    }

    // Personalized host provider service issued to the supplied provider.
    @Override
    protected TopologyProviderService createProviderService(TopologyProvider provider) {
        return new InternalTopologyProviderService(provider);
    }

    private class InternalTopologyProviderService
            extends AbstractProviderService<TopologyProvider>
            implements TopologyProviderService {

        InternalTopologyProviderService(TopologyProvider provider) {
            super(provider);
        }

        @Override
        public void topologyChanged(GraphDescription topoDescription,
                                    List<Event> reasons) {
            checkNotNull(topoDescription, "Topology description cannot be null");

            TopologyEvent event = store.updateTopology(provider().id(),
                                                       topoDescription, reasons);
            if (event != null) {
                log.info("Topology {} changed", event.subject());
                post(event);
            }
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements TopologyStoreDelegate {
        @Override
        public void notify(TopologyEvent event) {
            post(event);
        }
    }
}
