/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.nodemetrics.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.nodemetrics.NodeCpu;
import org.onosproject.nodemetrics.NodeDiskUsage;
import org.onosproject.nodemetrics.NodeMemory;
import org.onosproject.nodemetrics.NodeMetricsService;
import org.onosproject.nodemetrics.Units;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Component(immediate = true)
public class NodeMetricsManager implements NodeMetricsService {
    private static final int DEFAULT_POLL_FREQUENCY_SECONDS = 15;
    private static final String SLASH = "/";
    private static final Double PERCENTAGE_MULTIPLIER = 100.0;
    private final Logger log = LoggerFactory
            .getLogger(this.getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private ScheduledExecutorService metricsExecutor;
    private ScheduledFuture<?> scheduledTask;

    private ApplicationId appId;
    private NodeId localNodeId;

    private EventuallyConsistentMap<NodeId, NodeMemory> memoryStore;
    private EventuallyConsistentMap<NodeId, NodeDiskUsage> diskStore;
    private EventuallyConsistentMap<NodeId, NodeCpu> cpuStore;

    private Sigar sigar;

    @Activate
    public void activate() {
        appId = coreService
                .registerApplication("org.onosproject.nodemetrics");
        metricsExecutor = Executors.newSingleThreadScheduledExecutor(
                Tools.groupedThreads("nodemetrics/pollingStatics",
                        "statistics-executor-%d", log));

        localNodeId = clusterService.getLocalNode().id();
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(NodeMemory.class)
                .register(NodeDiskUsage.class)
                .register(NodeCpu.class)
                .register(Units.class);
        memoryStore = storageService.<NodeId, NodeMemory>eventuallyConsistentMapBuilder()
                .withSerializer(serializer)
                .withTimestampProvider((nodeId, memory) -> clockService.getTimestamp())
                .withName("nodemetrics-memory")
                .build();

        diskStore = storageService.<NodeId, NodeDiskUsage>eventuallyConsistentMapBuilder()
                .withSerializer(serializer)
                .withTimestampProvider((nodeId, disk) -> clockService.getTimestamp())
                .withName("nodemetrics-disk")
                .build();

        cpuStore = storageService.<NodeId, NodeCpu>eventuallyConsistentMapBuilder()
                .withSerializer(serializer)
                .withTimestampProvider((nodeId, cpu) -> clockService.getTimestamp())
                .withName("nodemetrics-cpu")
                .build();

        scheduledTask = schedulePolling();
        sigar = new Sigar();
        pollMetrics();
    }

    @Deactivate
    public void deactivate() {
        scheduledTask.cancel(true);
        metricsExecutor.shutdown();
        sigar.close();
    }

    @Override
    public Map<NodeId, NodeMemory> memory() {
        return this.ecToMap(memoryStore);
    }

    @Override
    public Map<NodeId, NodeDiskUsage> disk() {
        return this.ecToMap(diskStore);
    }

    @Override
    public Map<NodeId, NodeCpu> cpu() {
        return this.ecToMap(cpuStore);
    }

    @Override
    public NodeMemory memory(NodeId nodeid) {
        return memoryStore.get(nodeid);
    }

    @Override
    public NodeDiskUsage disk(NodeId nodeid) {
        return diskStore.get(nodeid);
    }

    @Override
    public NodeCpu cpu(NodeId nodeid) {
        return cpuStore.get(nodeid);
    }

    private ScheduledFuture schedulePolling() {
        return metricsExecutor.scheduleAtFixedRate(this::pollMetrics,
                DEFAULT_POLL_FREQUENCY_SECONDS / 4,
                DEFAULT_POLL_FREQUENCY_SECONDS, TimeUnit.SECONDS);
    }

    private <K, V> Map<K, V> ecToMap(EventuallyConsistentMap<K, V> ecMap) {
        return ecMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private void pollMetrics() {
        try {
            CpuPerc cpu = sigar.getCpuPerc();
            Mem mem = sigar.getMem();
            FileSystemUsage disk = sigar.getFileSystemUsage(SLASH);

            NodeMemory memoryNode = new NodeMemory.Builder().free(mem.getFree())
                    .used(mem.getUsed()).total(mem.getTotal()).withUnit(Units.BYTES)
                    .withNode(localNodeId).build();
            NodeCpu cpuNode = new NodeCpu.Builder().withNode(localNodeId)
                    .usage(cpu.getCombined() * PERCENTAGE_MULTIPLIER).build();
            NodeDiskUsage diskNode = new NodeDiskUsage.Builder().withNode(localNodeId)
                    .free(disk.getFree()).used(disk.getUsed()).withUnit(Units.KBYTES)
                    .total(disk.getTotal()).build();
            diskStore.put(localNodeId, diskNode);
            memoryStore.put(localNodeId, memoryNode);
            cpuStore.put(localNodeId, cpuNode);

        } catch (SigarException e) {
            log.error("Exception occurred ", e);
        }

    }
}
