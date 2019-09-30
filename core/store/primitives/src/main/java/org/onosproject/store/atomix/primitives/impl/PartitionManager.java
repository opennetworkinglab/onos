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

package org.onosproject.store.atomix.primitives.impl;

import io.atomix.cluster.MemberId;
import io.atomix.primitive.partition.PartitionGroup;
import io.atomix.protocols.raft.MultiRaftProtocol;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.PartitionId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.atomix.impl.AtomixManager;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.primitives.PartitionEvent;
import org.onosproject.store.primitives.PartitionEventListener;
import org.onosproject.store.primitives.PartitionService;
import org.onosproject.store.service.PartitionClientInfo;
import org.onosproject.store.service.PartitionInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.PARTITION_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@code PartitionService} and {@code PartitionAdminService}.
 */
@Component(service = { PartitionService.class, PartitionAdminService.class })
public class PartitionManager extends AbstractListenerManager<PartitionEvent, PartitionEventListener>
    implements PartitionService, PartitionAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AtomixManager atomixManager;

    private PartitionGroup partitionGroup;

    @Activate
    public void activate() {
        partitionGroup = atomixManager.getAtomix().getPartitionService().getPartitionGroup(MultiRaftProtocol.TYPE);
        eventDispatcher.addSink(PartitionEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(PartitionEvent.class);
        log.info("Stopped");
    }

    @Override
    public int getNumberOfPartitions() {
        checkPermission(PARTITION_READ);
        return partitionGroup.getPartitions().size();
    }

    @Override
    public Set<PartitionId> getAllPartitionIds() {
        checkPermission(PARTITION_READ);
        return partitionGroup.getPartitionIds().stream()
            .map(partitionId -> PartitionId.from(partitionId.id()))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<NodeId> getConfiguredMembers(PartitionId partitionId) {
        checkPermission(PARTITION_READ);
        io.atomix.primitive.partition.PartitionId atomixPartitionId =
            io.atomix.primitive.partition.PartitionId.from(partitionGroup.name(), partitionId.id());
        return partitionGroup.getPartition(atomixPartitionId).members()
            .stream()
            .map(member -> NodeId.nodeId(member.id()))
            .collect(Collectors.toSet());
    }

    @Override
    public List<PartitionInfo> partitionInfo() {
        checkPermission(PARTITION_READ);
        return partitionGroup.getPartitions()
            .stream()
            .map(partition -> {
                MemberId primary = partition.primary();
                return new PartitionInfo(
                    PartitionId.from(partition.id().id()),
                    partition.term(),
                    partition.members().stream().map(member -> member.id()).collect(Collectors.toList()),
                    primary != null ? primary.id() : null);
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<PartitionClientInfo> partitionClientInfo() {
        checkPermission(PARTITION_READ);
        return partitionGroup.getPartitions()
            .stream()
            .map(partition -> new PartitionClientInfo(
                PartitionId.from(partition.id().id()),
                partition.members().stream()
                    .map(member -> NodeId.nodeId(member.id()))
                    .collect(Collectors.toList())))
            .collect(Collectors.toList());
    }

    @Override
    public void snapshot() {
        checkPermission(PARTITION_READ);
        if (partitionGroup != null) {
            partitionGroup.snapshot().join();
        }
    }

    @Override
    public void snapshot(PartitionId partitionId) {
        checkPermission(PARTITION_READ);
        io.atomix.primitive.partition.PartitionId atomixPartitionId =
                io.atomix.primitive.partition.PartitionId.from(partitionGroup.name(), partitionId.id());
        if (partitionGroup != null &&
                partitionGroup.getPartition(atomixPartitionId) != null) {
            partitionGroup.snapshot(atomixPartitionId).join();
        }
    }
}
