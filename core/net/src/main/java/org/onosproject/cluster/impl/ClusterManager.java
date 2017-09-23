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
package org.onosproject.cluster.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.UnifiedClusterAdminService;
import org.onosproject.cluster.UnifiedClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.onosproject.core.VersionService;
import org.slf4j.Logger;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the cluster service.
 */
@Component(immediate = true)
@Service
public class ClusterManager implements ClusterService, ClusterAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private UnifiedClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private UnifiedClusterAdminService clusterAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private VersionService versionService;

    private Version version;

    @Activate
    public void activate() {
        version = versionService.version();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public ControllerNode getLocalNode() {
        checkPermission(CLUSTER_READ);
        return clusterService.getLocalNode();
    }

    @Override
    public Set<ControllerNode> getNodes() {
        checkPermission(CLUSTER_READ);
        return clusterService.getNodes()
                .stream()
                .filter(node -> clusterService.getVersion(node.id()).equals(version))
                .collect(Collectors.toSet());
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        Version nodeVersion = clusterService.getVersion(nodeId);
        if (nodeVersion != null && nodeVersion.equals(version)) {
            return clusterService.getNode(nodeId);
        }
        return null;
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        Version nodeVersion = clusterService.getVersion(nodeId);
        if (nodeVersion != null && nodeVersion.equals(version)) {
            return clusterService.getState(nodeId);
        }
        return null;
    }

    @Override
    public Version getVersion(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        Version nodeVersion = clusterService.getVersion(nodeId);
        if (nodeVersion != null && nodeVersion.equals(version)) {
            return nodeVersion;
        }
        return null;
    }

    @Override
    public void markFullyStarted(boolean started) {
        clusterAdminService.markFullyStarted(started);
    }

    @Override
    public DateTime getLastUpdated(NodeId nodeId) {
        checkPermission(CLUSTER_READ);
        Version nodeVersion = clusterService.getVersion(nodeId);
        if (nodeVersion != null && nodeVersion.equals(version)) {
            return clusterService.getLastUpdated(nodeId);
        }
        return null;
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes) {
        clusterAdminService.formCluster(nodes);
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes, int partitionSize) {
        clusterAdminService.formCluster(nodes, partitionSize);
    }

    @Override
    public ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort) {
        return clusterAdminService.addNode(nodeId, ip, tcpPort);
    }

    @Override
    public void removeNode(NodeId nodeId) {
        Version nodeVersion = clusterService.getVersion(nodeId);
        if (nodeVersion != null && nodeVersion.equals(version)) {
            clusterAdminService.removeNode(nodeId);
        }
    }

    @Override
    public void addListener(ClusterEventListener listener) {
        clusterService.addListener(listener);
    }

    @Override
    public void removeListener(ClusterEventListener listener) {
        clusterService.removeListener(listener);
    }
}
