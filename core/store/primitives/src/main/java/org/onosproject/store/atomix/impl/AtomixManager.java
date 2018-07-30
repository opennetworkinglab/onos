/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.impl;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atomix manager.
 */
@Component(immediate = true)
@Service(value = AtomixManager.class)
public class AtomixManager {
    private static final String LOCAL_DATA_DIR = System.getProperty("karaf.data") + "/db/partitions/";
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService metadataService;

    private Atomix atomix;

    /**
     * Returns the Atomix instance.
     *
     * @return the Atomix instance
     */
    public Atomix getAtomix() {
        return atomix;
    }

    @Activate
    public void activate() {
        log.info("{}", metadataService.getClusterMetadata());
        atomix = createAtomix();
        atomix.start().join();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        atomix.stop().join();
        log.info("Stopped");
    }

    private Atomix createAtomix() {
        ClusterMetadata metadata = metadataService.getClusterMetadata();
        if (!metadata.getStorageNodes().isEmpty()) {
            // If storage nodes are defined, construct an instance that connects to them for service discovery.
            return Atomix.builder(getClass().getClassLoader())
                .withClusterId(metadata.getName())
                .withMemberId(metadataService.getLocalNode().id().id())
                .withAddress(metadataService.getLocalNode().ip().toString(), metadataService.getLocalNode().tcpPort())
                .withProperty("type", "onos")
                .withMembershipProvider(BootstrapDiscoveryProvider.builder()
                    .withNodes(metadata.getStorageNodes().stream()
                        .map(node -> io.atomix.cluster.Node.builder()
                            .withId(node.id().id())
                            .withAddress(node.ip().toString(), node.tcpPort())
                            .build())
                        .collect(Collectors.toList()))
                    .build())
                .build();
        } else {
            log.warn("No storage nodes found in cluster metadata!");
            log.warn("Bootstrapping ONOS cluster in test mode! For production use, configure external storage nodes.");

            // If storage nodes are not defined, construct a local instance with a Raft partition group.
            List<String> raftMembers = !metadata.getControllerNodes().isEmpty()
                ? metadata.getControllerNodes()
                .stream()
                .map(node -> node.id().id())
                .collect(Collectors.toList())
                : Collections.singletonList(metadataService.getLocalNode().id().id());
            return Atomix.builder(getClass().getClassLoader())
                .withClusterId(metadata.getName())
                .withMemberId(metadataService.getLocalNode().id().id())
                .withAddress(metadataService.getLocalNode().ip().toString(), metadataService.getLocalNode().tcpPort())
                .withProperty("type", "onos")
                .withMembershipProvider(BootstrapDiscoveryProvider.builder()
                    .withNodes(metadata.getControllerNodes().stream()
                        .map(node -> io.atomix.cluster.Node.builder()
                            .withId(node.id().id())
                            .withAddress(node.ip().toString(), node.tcpPort())
                            .build())
                        .collect(Collectors.toList()))
                    .build())
                .withManagementGroup(RaftPartitionGroup.builder("system")
                    .withNumPartitions(1)
                    .withDataDirectory(new File(LOCAL_DATA_DIR, "system"))
                    .withMembers(raftMembers)
                    .build())
                .addPartitionGroup(RaftPartitionGroup.builder("raft")
                    .withNumPartitions(raftMembers.size())
                    .withDataDirectory(new File(LOCAL_DATA_DIR, "data"))
                    .withMembers(raftMembers)
                    .build())
                .build();
        }
    }
}
