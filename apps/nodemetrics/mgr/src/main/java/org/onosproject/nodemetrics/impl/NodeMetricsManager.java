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

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.nodemetrics.NodeCpuUsage;
import org.onosproject.nodemetrics.NodeDiskUsage;
import org.onosproject.nodemetrics.NodeMemoryUsage;
import org.onosproject.nodemetrics.NodeMetricsService;
import org.onosproject.nodemetrics.Units;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.getIntegerProperty;
import static org.onosproject.nodemetrics.impl.OsgiPropertyConstants.METRIC_POLL_FREQUENCY_SECONDS;
import static org.onosproject.nodemetrics.impl.OsgiPropertyConstants.METRIC_POLL_FREQUENCY_SECONDS_DEFAULT;


@Component(
    immediate = true,
    service = NodeMetricsService.class,
    property = {
        METRIC_POLL_FREQUENCY_SECONDS + ":Integer=" + METRIC_POLL_FREQUENCY_SECONDS_DEFAULT
    }
)
public class NodeMetricsManager implements NodeMetricsService {

    private static final String SLASH = "/";
    private static final Double PERCENTAGE_MULTIPLIER = 100.0;
    private final Logger log = LoggerFactory
            .getLogger(this.getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    private ScheduledExecutorService metricsExecutor;
    private ScheduledFuture<?> scheduledTask;

    private ApplicationId appId;
    private NodeId localNodeId;

    private EventuallyConsistentMap<NodeId, NodeMemoryUsage> memoryStore;
    private EventuallyConsistentMap<NodeId, NodeDiskUsage> diskStore;
    private EventuallyConsistentMap<NodeId, NodeCpuUsage> cpuStore;

    private Sigar sigar;

    /** Frequency (in seconds) for polling controller metrics. */
    protected int metricPollFrequencySeconds = METRIC_POLL_FREQUENCY_SECONDS_DEFAULT;

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService
                .registerApplication("org.onosproject.nodemetrics");
        cfgService.registerProperties(getClass());
        metricsExecutor = Executors.newSingleThreadScheduledExecutor(
                Tools.groupedThreads("nodemetrics/pollingStatics",
                        "statistics-executor-%d", log));

        localNodeId = clusterService.getLocalNode().id();
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(NodeMemoryUsage.class)
                .register(NodeDiskUsage.class)
                .register(NodeCpuUsage.class)
                .register(Units.class);
        memoryStore = storageService.<NodeId, NodeMemoryUsage>eventuallyConsistentMapBuilder()
                .withSerializer(serializer)
                .withTimestampProvider((nodeId, memory) -> clockService.getTimestamp())
                .withName("nodemetrics-memory")
                .build();

        diskStore = storageService.<NodeId, NodeDiskUsage>eventuallyConsistentMapBuilder()
                .withSerializer(serializer)
                .withTimestampProvider((nodeId, disk) -> clockService.getTimestamp())
                .withName("nodemetrics-disk")
                .build();

        cpuStore = storageService.<NodeId, NodeCpuUsage>eventuallyConsistentMapBuilder()
                .withSerializer(serializer)
                .withTimestampProvider((nodeId, cpu) -> clockService.getTimestamp())
                .withName("nodemetrics-cpu")
                .build();
        modified(context);
        sigar = new Sigar();
        pollMetrics();
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        scheduledTask.cancel(true);
        metricsExecutor.shutdown();
        sigar.close();
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            log.info("No component configuration");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        int newPollFrequency = getNewPollFrequency(properties);
        //First time call to this modified method is when app activates
        if (Objects.isNull(scheduledTask)) {
            metricPollFrequencySeconds = newPollFrequency;
            scheduledTask = schedulePolling();
        } else {
            if (newPollFrequency != metricPollFrequencySeconds) {
                metricPollFrequencySeconds = newPollFrequency;
                //stops the old scheduled task
                scheduledTask.cancel(true);
                //schedules new task at the new polling rate
                scheduledTask = schedulePolling();
            }
        }
    }

    @Override
    public Map<NodeId, NodeMemoryUsage> memory() {
        return this.ecToMap(memoryStore);
    }

    @Override
    public Map<NodeId, NodeDiskUsage> disk() {
        return this.ecToMap(diskStore);
    }

    @Override
    public Map<NodeId, NodeCpuUsage> cpu() {
        return this.ecToMap(cpuStore);
    }

    @Override
    public NodeMemoryUsage memory(NodeId nodeid) {
        return memoryStore.get(nodeid);
    }

    @Override
    public NodeDiskUsage disk(NodeId nodeid) {
        return diskStore.get(nodeid);
    }

    @Override
    public NodeCpuUsage cpu(NodeId nodeid) {
        return cpuStore.get(nodeid);
    }

    private ScheduledFuture schedulePolling() {
        return metricsExecutor.scheduleAtFixedRate(this::pollMetrics,
                metricPollFrequencySeconds / 4,
                metricPollFrequencySeconds, TimeUnit.SECONDS);
    }

    private int getNewPollFrequency(Dictionary<?, ?> properties) {
        int newPollFrequency;
        try {
            newPollFrequency = getIntegerProperty(properties, METRIC_POLL_FREQUENCY_SECONDS);
            //String s = getIntegerProperty(properties, "metricPollFrequencySeconds");
            //newPollFrequency = isNullOrEmpty(s) ? pollFrequency : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPollFrequency = METRIC_POLL_FREQUENCY_SECONDS_DEFAULT;
        }
        return newPollFrequency;
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

            NodeMemoryUsage memoryNode = new NodeMemoryUsage.Builder().free(mem.getFree())
                    .used(mem.getUsed()).total(mem.getTotal()).withUnit(Units.BYTES)
                    .withNode(localNodeId).build();
            NodeCpuUsage cpuNode = new NodeCpuUsage.Builder().withNode(localNodeId)
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
