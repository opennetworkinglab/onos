/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.topology;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;

import java.util.Map;
import java.util.Set;

/**
 * Test adapter for topology service.
 */
public class TopologyServiceAdapter implements TopologyService {
    @Override
    public Topology currentTopology() {
        return null;
    }

    @Override
    public boolean isLatest(Topology topology) {
        return false;
    }

    @Override
    public TopologyGraph getGraph(Topology topology) {
        return null;
    }

    @Override
    public Set<TopologyCluster> getClusters(Topology topology) {
        return null;
    }

    @Override
    public TopologyCluster getCluster(Topology topology, ClusterId clusterId) {
        return null;
    }

    @Override
    public Set<DeviceId> getClusterDevices(Topology topology,
                                           TopologyCluster cluster) {
        return null;
    }

    @Override
    public Set<Link> getClusterLinks(Topology topology,
                                     TopologyCluster cluster) {
        return null;
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        return null;
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst,
                              LinkWeight weight) {
        return null;
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst,
                              LinkWeigher weigher) {
        return null;
    }

    @Override
    public boolean isInfrastructure(Topology topology,
                                    ConnectPoint connectPoint) {
        return false;
    }

    @Override
    public boolean isBroadcastPoint(Topology topology,
                                    ConnectPoint connectPoint) {
        return false;
    }

    @Override
    public void addListener(TopologyListener listener) {
    }

    @Override
    public void removeListener(TopologyListener listener) {
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              LinkWeight weight) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              LinkWeigher weigher) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              Map<Link, Object> riskProfile) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst, LinkWeight weight,
                                              Map<Link, Object> riskProfile) {
        return null;
    }

    @Override
    public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                              DeviceId dst,
                                              LinkWeigher weigher,
                                              Map<Link, Object> riskProfile) {
        return null;
    }

}
