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

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VnetService;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.AbstractPathService;
import org.onosproject.net.topology.TopologyService;

import java.util.Map;
import java.util.Set;

/**
 * Path service implementation built on the virtual network service.
 */
public class VirtualNetworkPathManager
        extends AbstractPathService
        implements PathService, VnetService {

    private final NetworkId networkId;

    /**
     * Creates a new virtual network path service object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param networkId a virtual network identifier
     */

    public VirtualNetworkPathManager(VirtualNetworkService virtualNetworkManager,
                                     NetworkId networkId) {
        this.networkId = networkId;

        topologyService = virtualNetworkManager.get(networkId(), TopologyService.class);
        hostService = virtualNetworkManager.get(networkId(), HostService.class);
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst) {
        return super.getPaths(src, dst, (LinkWeigher) null);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst) {
        return getDisjointPaths(src, dst, (LinkWeigher) null);
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst,
                                              Map<Link, Object> riskProfile) {
        return getDisjointPaths(src, dst, (LinkWeigher) null, riskProfile);
    }

    @Override
    public NetworkId networkId() {
        return this.networkId;
    }
}
