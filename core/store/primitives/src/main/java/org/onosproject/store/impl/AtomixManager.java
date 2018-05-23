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
package org.onosproject.store.impl;

import java.util.stream.Collectors;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.core.Atomix;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atomix manager.
 */
@Component(immediate = true)
@Service(value = AtomixManager.class)
public class AtomixManager {

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
        return Atomix.builder(getClass().getClassLoader())
            .withClusterId(metadataService.getClusterMetadata().getName())
            .withMemberId(metadataService.getLocalNode().id().id())
            .withAddress(metadataService.getLocalNode().ip().toString(), metadataService.getLocalNode().tcpPort())
            .withProperty("type", "onos")
            .withMembershipProvider(BootstrapDiscoveryProvider.builder()
                .withNodes(metadataService.getClusterMetadata().getStorageNodes().stream()
                    .map(node -> io.atomix.cluster.Node.builder()
                        .withId(node.id().id())
                        .withAddress(node.ip().toString(), node.tcpPort())
                        .build())
                    .collect(Collectors.toList()))
                .build())
            .build();
    }
}
