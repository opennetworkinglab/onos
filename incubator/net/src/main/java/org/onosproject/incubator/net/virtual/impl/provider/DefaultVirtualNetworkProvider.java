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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualLink;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProviderRegistry;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProviderService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Virtual network topology provider.
 */
@Component(immediate = true)
@Service
public class DefaultVirtualNetworkProvider
        extends AbstractProvider implements VirtualNetworkProvider {

    private final Logger log = getLogger(DefaultVirtualNetworkProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkProviderRegistry providerRegistry;

    private VirtualNetworkProviderService providerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    protected TopologyListener topologyListener = new InternalTopologyListener();

    private ExecutorService executor;

    /**
     * Default constructor.
     */
    public DefaultVirtualNetworkProvider() {
        super(DefaultVirtualLink.PID);
    }

    @Activate
    public void activate() {
        executor = newSingleThreadExecutor(groupedThreads("onos/vnet", "provider", log));
        providerService = providerRegistry.register(this);
        topologyService.addListener(topologyListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        topologyService.removeListener(topologyListener);
        executor.shutdownNow();
        executor = null;
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public boolean isTraversable(ConnectPoint src, ConnectPoint dst) {
        final boolean[] foundSrc = new boolean[1];
        final boolean[] foundDst = new boolean[1];
        Topology topology = topologyService.currentTopology();
        Set<Path> paths = topologyService.getPaths(topology, src.deviceId(), dst.deviceId());
        paths.forEach(path -> {
            foundDst[0] = false;
            foundSrc[0] = false;
            // Traverse the links in each path to determine if both the src and dst connection
            // point are in the path, if so then this src/dst pair are traversable.
            path.links().forEach(link -> {
                if (link.src().equals(src)) {
                    foundSrc[0] = true;
                }
                if (link.dst().equals(dst)) {
                    foundDst[0] = true;
                }
            });
            if (foundSrc[0] && foundDst[0]) {
                return;
            }
        });
        return foundSrc[0] && foundDst[0];
    }

    @Override
    public TunnelId createTunnel(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        return null;
    }

    @Override
    public void destroyTunnel(NetworkId networkId, TunnelId tunnelId) {

    }

    /**
     * Returns a set of set of interconnected connect points in the default topology.
     * The inner set represents the interconnected connect points, and the outerset
     * represents separate clusters.
     *
     * @param topology the default topology
     * @return set of set of interconnected connect points.
     */
    public Set<Set<ConnectPoint>> getConnectPoints(Topology topology) {
        Set<Set<ConnectPoint>> clusters = new HashSet<>();
        Set<TopologyCluster> topologyClusters = topologyService.getClusters(topology);
        topologyClusters.forEach(topologyCluster -> {
            Set<ConnectPoint> connectPointSet = new HashSet<>();
            Set<Link> clusterLinks =
                    topologyService.getClusterLinks(topology, topologyCluster);
            clusterLinks.forEach(link -> {
                connectPointSet.add(link.src());
                connectPointSet.add(link.dst());
            });
            if (!connectPointSet.isEmpty()) {
                clusters.add(connectPointSet);
            }
        });
        return clusters;
    }

    /**
     * Topology event listener.
     */
    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            // Perform processing off the listener thread.
            executor.submit(() -> providerService
                    .topologyChanged(getConnectPoints(event.subject())));
        }

        @Override
        public boolean isRelevant(TopologyEvent event) {
            return event.type() == TopologyEvent.Type.TOPOLOGY_CHANGED &&
                    event.reasons().stream().anyMatch(reason -> reason instanceof LinkEvent);
        }
    }
}
