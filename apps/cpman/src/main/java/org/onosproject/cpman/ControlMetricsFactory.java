/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.cpman;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.metrics.MetricsService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class to provide various control plane metrics to other components.
 */
public final class ControlMetricsFactory {
    private static volatile ControlMetricsFactory uniqueInstance;

    private MetricsService metricsService;
    private boolean enableMonitor = false;

    // define a ControlMetricsSystemSpec
    private ControlMetricsSystemSpec cmss;

    // define a set of MetricsAggregators
    private MetricsAggregator cpuLoad;
    private MetricsAggregator totalCpuTime;
    private MetricsAggregator sysCpuTime;
    private MetricsAggregator userCpuTime;
    private MetricsAggregator cpuIdleTime;
    private MetricsAggregator memoryUsed;
    private MetricsAggregator memoryFree;
    private MetricsAggregator memoryUsedPercentage;
    private MetricsAggregator memoryFreePercentage;
    private Map<String, MetricsAggregator> diskReadBytes;
    private Map<String, MetricsAggregator> diskWriteBytes;
    private Map<String, MetricsAggregator> nwIncomingBytes;
    private Map<String, MetricsAggregator> nwOutgoingBytes;
    private Map<String, MetricsAggregator> nwIncomingPackets;
    private Map<String, MetricsAggregator> nwOutgoingPackets;

    private Map<DeviceId, MetricsAggregator> inboundPacket;
    private Map<DeviceId, MetricsAggregator> outboundPacket;
    private Map<DeviceId, MetricsAggregator> flowmodPacket;
    private Map<DeviceId, MetricsAggregator> flowrmvPacket;
    private Map<DeviceId, MetricsAggregator> requestPacket;
    private Map<DeviceId, MetricsAggregator> replyPacket;
    private Set<DeviceId> deviceIds = Sets.newConcurrentHashSet();
    private Set<String> diskPartitions = Sets.newConcurrentHashSet();
    private Set<String> nwInterfaces = Sets.newConcurrentHashSet();

    private ControlMetricsFactory(MetricsService metricsService, DeviceService deviceService) {
        this.metricsService = metricsService;
        registerMetrics();

        deviceService.getDevices().forEach(d->deviceIds.add(d.id()));

        addAllControlMessageMetrics(deviceIds);
    }

    public static ControlMetricsFactory getInstance(MetricsService metricsService,
                                                    DeviceService deviceService) {
        if (uniqueInstance == null) {
            synchronized (ControlMetricsFactory.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new ControlMetricsFactory(metricsService, deviceService);
                }
            }
        }
        return uniqueInstance;
    }

    /**
     * Sets system specification.
     *
     * @param cmss ControlMetricsSystemSpec object
     */
    public void setSystemSpec(ControlMetricsSystemSpec cmss) {
        if (this.cmss == null) {
            this.cmss = cmss;
        }
    }

    /**
     * Adds control metrics of a new device.
     *
     * @param deviceId {@link org.onosproject.net.DeviceId}
     */
    public void addControlMessageMetricsByDeviceId(DeviceId deviceId) {
        MetricsAggregator inbound = new MetricsAggregator(metricsService,
                        ControlMetricType.INBOUND_PACKET, Optional.of(deviceId));
        MetricsAggregator outbound = new MetricsAggregator(metricsService,
                        ControlMetricType.OUTBOUND_PACKET, Optional.of(deviceId));
        MetricsAggregator flowmod = new MetricsAggregator(metricsService,
                        ControlMetricType.FLOW_MOD_PACKET, Optional.of(deviceId));
        MetricsAggregator flowrmv = new MetricsAggregator(metricsService,
                        ControlMetricType.FLOW_REMOVED_PACKET, Optional.of(deviceId));
        MetricsAggregator request = new MetricsAggregator(metricsService,
                        ControlMetricType.REQUEST_PACKET, Optional.of(deviceId));
        MetricsAggregator reply = new MetricsAggregator(metricsService,
                        ControlMetricType.REPLY_PACKET, Optional.of(deviceId));

        inboundPacket.putIfAbsent(deviceId, inbound);
        outboundPacket.putIfAbsent(deviceId, outbound);
        flowmodPacket.putIfAbsent(deviceId, flowmod);
        flowrmvPacket.putIfAbsent(deviceId, flowrmv);
        requestPacket.putIfAbsent(deviceId, request);
        replyPacket.putIfAbsent(deviceId, reply);

        deviceIds.add(deviceId);
    }

    /**
     * Adds control metrics of a disk.
     *
     * @param partitionName disk partition name
     */
    public void addDiskMetricsByPartition(String partitionName) {
        MetricsAggregator readBytes = new MetricsAggregator(metricsService,
                        ControlMetricType.DISK_READ_BYTES, partitionName);
        MetricsAggregator writeBytes = new MetricsAggregator(metricsService,
                ControlMetricType.DISK_WRITE_BYTES, partitionName);

        diskReadBytes.putIfAbsent(partitionName, readBytes);
        diskWriteBytes.putIfAbsent(partitionName, writeBytes);

        diskPartitions.add(partitionName);
    }

    /**
     * Adds control metrics of a ethernet interface.
     *
     * @param interfaceName network interface name
     */
    public void addNetworkMetricsByInterface(String interfaceName) {
        MetricsAggregator incomingBytes = new MetricsAggregator(metricsService,
                        ControlMetricType.NW_INCOMING_BYTES, interfaceName);
        MetricsAggregator outgoingBytes = new MetricsAggregator(metricsService,
                ControlMetricType.NW_OUTGOING_BYTES, interfaceName);
        MetricsAggregator incomingPackets = new MetricsAggregator(metricsService,
                ControlMetricType.NW_INCOMING_PACKETS, interfaceName);
        MetricsAggregator outgoingPackets = new MetricsAggregator(metricsService,
                ControlMetricType.NW_OUTGOING_PACKETS, interfaceName);

        nwIncomingBytes.putIfAbsent(interfaceName, incomingBytes);
        nwOutgoingBytes.putIfAbsent(interfaceName, outgoingBytes);
        nwIncomingPackets.putIfAbsent(interfaceName, incomingPackets);
        nwOutgoingPackets.putIfAbsent(interfaceName, outgoingPackets);

        nwInterfaces.add(interfaceName);
    }

    /**
     * Removes control metrics of an existing device.
     *
     * @param deviceId {@link org.onosproject.net.DeviceId}
     */
    public void removeControlMessageMetricsByDeviceId(DeviceId deviceId) {
        inboundPacket.remove(deviceId);
        outboundPacket.remove(deviceId);
        flowmodPacket.remove(deviceId);
        flowrmvPacket.remove(deviceId);
        requestPacket.remove(deviceId);
        replyPacket.remove(deviceId);

        deviceIds.remove(deviceId);
    }

    /**
     * Removes control metrics of a disk.
     *
     * @param partitionName disk partition name
     */
    public void removeDiskMetricsByResourceName(String partitionName) {
        diskReadBytes.remove(partitionName);
        diskWriteBytes.remove(partitionName);

        diskPartitions.remove(partitionName);
    }

    /**
     * Removes control metrics of a network interface.
     *
     * @param interfaceName network interface name
     */
    public void removeNetworkInterfacesByResourceName(String interfaceName) {
        nwIncomingBytes.remove(interfaceName);
        nwOutgoingBytes.remove(interfaceName);
        nwIncomingPackets.remove(interfaceName);
        nwOutgoingPackets.remove(interfaceName);

        nwInterfaces.remove(interfaceName);
    }

    /**
     * Returns all device ids.
     *
     * @return a collection of device id
     */
    public Set<DeviceId> getDeviceIds() {
        return ImmutableSet.copyOf(this.deviceIds);
    }

    /**
     * Returns all disk partition names.
     *
     * @return a collection of disk partition.
     */
    public Set<String> getDiskPartitions() {
        return ImmutableSet.copyOf(this.diskPartitions);
    }

    /**
     * Returns all network interface names.
     *
     * @return a collection of network interface.
     */
    public Set<String> getNetworkInterfaces() {
        return ImmutableSet.copyOf(this.nwInterfaces);
    }

    /**
     * Adds control metrics for all devices.
     *
     * @param deviceIds a set of deviceIds
     */
    public void addAllControlMessageMetrics(Set<DeviceId> deviceIds) {
        deviceIds.forEach(v -> addControlMessageMetricsByDeviceId(v));
    }

    /**
     * Returns monitoring status.
     *
     * @return monitoring status
     */
    public boolean isMonitor() {
        return this.enableMonitor;
    }

    /**
     * Enable control plane monitoring.
     */
    protected void startMonitor() {
        this.enableMonitor = true;
    }

    /**
     * Disable control plane monitoring.
     */
    protected void stopMonitor() {
        this.enableMonitor = false;
    }

    /**
     * Registers new control metrics.
     */
    protected void registerMetrics() {
        /* CPU */
        cpuLoad = new MetricsAggregator(metricsService, ControlMetricType.CPU_LOAD);
        totalCpuTime = new MetricsAggregator(metricsService, ControlMetricType.TOTAL_CPU_TIME);
        sysCpuTime = new MetricsAggregator(metricsService, ControlMetricType.SYS_CPU_TIME);
        userCpuTime = new MetricsAggregator(metricsService, ControlMetricType.USER_CPU_TIME);
        cpuIdleTime = new MetricsAggregator(metricsService, ControlMetricType.CPU_IDLE_TIME);

        /* Memory */
        memoryFree = new MetricsAggregator(metricsService, ControlMetricType.MEMORY_FREE);
        memoryUsed = new MetricsAggregator(metricsService, ControlMetricType.MEMORY_USED);
        memoryFreePercentage = new MetricsAggregator(metricsService,
                                        ControlMetricType.MEMORY_FREE_PERCENTAGE);
        memoryUsedPercentage = new MetricsAggregator(metricsService,
                                        ControlMetricType.MEMORY_USED_PERCENTAGE);

        /* Disk I/O */
        diskReadBytes = new ConcurrentHashMap<>();
        diskWriteBytes = new ConcurrentHashMap<>();

        /* Network I/O */
        nwIncomingBytes = new ConcurrentHashMap<>();
        nwOutgoingBytes = new ConcurrentHashMap<>();
        nwIncomingPackets = new ConcurrentHashMap<>();
        nwOutgoingPackets = new ConcurrentHashMap<>();

        /* OpenFlow Messages */
        inboundPacket = new ConcurrentHashMap<>();
        outboundPacket = new ConcurrentHashMap<>();
        flowmodPacket = new ConcurrentHashMap<>();
        flowrmvPacket = new ConcurrentHashMap<>();
        requestPacket = new ConcurrentHashMap<>();
        replyPacket = new ConcurrentHashMap<>();
    }

    /**
     * Unregisters all control metrics.
     */
    protected void unregisterMetrics() {
        /* Disk I/O */
        diskReadBytes.clear();
        diskWriteBytes.clear();

        /* Network I/O */
        nwIncomingBytes.clear();
        nwOutgoingBytes.clear();
        nwIncomingPackets.clear();
        nwOutgoingPackets.clear();

        /* OpenFlow Message */
        inboundPacket.clear();
        outboundPacket.clear();
        flowmodPacket.clear();
        flowrmvPacket.clear();
        requestPacket.clear();
        replyPacket.clear();
    }

    public MetricsAggregator cpuLoadMetric() {
        return cpuLoad;
    }

    public MetricsAggregator totalCpuTimeMetric() {
        return totalCpuTime;
    }

    public MetricsAggregator sysCpuTimeMetric() {
        return sysCpuTime;
    }

    public MetricsAggregator userCpuTime() {
        return userCpuTime;
    }

    public MetricsAggregator cpuIdleTime() {
        return cpuIdleTime;
    }

    public MetricsAggregator memoryFreePercentage() {
        return memoryFreePercentage;
    }

    public MetricsAggregator memoryUsedPercentage() {
        return memoryUsedPercentage;
    }

    public MetricsAggregator diskReadBytes(String partitionName) {
        return diskReadBytes.get(partitionName);
    }

    public MetricsAggregator diskWriteBytes(String partitionName) {
        return diskWriteBytes.get(partitionName);
    }

    public MetricsAggregator nwIncomingBytes(String interfaceName) {
        return nwIncomingBytes.get(interfaceName);
    }

    public MetricsAggregator nwOutgoingBytes(String interfaceName) {
        return nwOutgoingBytes.get(interfaceName);
    }

    public MetricsAggregator nwIncomingPackets(String interfaceName) {
        return nwIncomingPackets.get(interfaceName);
    }

    public MetricsAggregator nwOutgoingPackets(String interfaceName) {
        return nwOutgoingPackets.get(interfaceName);
    }

    public Map<DeviceId, MetricsAggregator> inboundPacket() {
        return ImmutableMap.copyOf(inboundPacket);
    }

    public Map<DeviceId, MetricsAggregator> outboundPacket() {
        return ImmutableMap.copyOf(outboundPacket);
    }

    public Map<DeviceId, MetricsAggregator> flowmodPacket() {
        return ImmutableMap.copyOf(flowmodPacket);
    }

    public Map<DeviceId, MetricsAggregator> flowrmvPacket() {
        return ImmutableMap.copyOf(flowrmvPacket);
    }

    public Map<DeviceId, MetricsAggregator> requestPacket() {
        return ImmutableMap.copyOf(requestPacket);
    }

    public Map<DeviceId, MetricsAggregator> replyPacket() {
        return ImmutableMap.copyOf(replyPacket);
    }

    public MetricsAggregator inboundPacket(DeviceId deviceId) {
        return inboundPacket.get(deviceId);
    }

    public MetricsAggregator outboundPacket(DeviceId deviceId) {
        return outboundPacket.get(deviceId);
    }

    public MetricsAggregator flowmodPacket(DeviceId deviceId) {
        return flowmodPacket.get(deviceId);
    }

    public MetricsAggregator flowrmvPacket(DeviceId deviceId) {
        return flowrmvPacket.get(deviceId);
    }

    public MetricsAggregator requestPacket(DeviceId deviceId) {
        return requestPacket.get(deviceId);
    }

    public MetricsAggregator replyPacket(DeviceId deviceId) {
        return replyPacket.get(deviceId);
    }
}