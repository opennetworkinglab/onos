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
package org.onosproject.incubator.net.virtual.impl;

import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.AbstractPathService;
import org.onosproject.net.topology.TopologyService;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Path service implementation built on the virtual network service.
 */
public class VirtualNetworkPathService extends AbstractPathService
        implements PathService, VnetService {

    private static final String NETWORK_NULL = "Network ID cannot be null";

    private final VirtualNetwork network;

    /**
     * Creates a new virtual network path service object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param network               virtual network
     */

    public VirtualNetworkPathService(VirtualNetworkService virtualNetworkManager, VirtualNetwork network) {
        checkNotNull(network, NETWORK_NULL);
        this.network = network;
        topologyService = virtualNetworkManager.get(network.id(), TopologyService.class);
        hostService = virtualNetworkManager.get(network.id(), HostService.class);
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst) {
        return getPaths(src, dst, null);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst) {
        return getDisjointPaths(src, dst, (LinkWeight) null);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst, Map<Link, Object> riskProfile) {
        return getDisjointPaths(src, dst, null, riskProfile);
    }

    @Override
    public VirtualNetwork network() {
        return network;
    }
}
