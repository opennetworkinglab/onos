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

package org.onosproject.drivers.server;

import org.onosproject.drivers.server.behavior.CpuStatisticsDiscovery;
import org.onosproject.drivers.server.behavior.MonitoringStatisticsDiscovery;
import org.onosproject.drivers.server.devices.cpu.CpuCacheHierarchyDevice;
import org.onosproject.drivers.server.devices.cpu.CpuDevice;
import org.onosproject.drivers.server.devices.memory.MemoryHierarchyDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.ServerDeviceDescription;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.stats.MemoryStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.drivers.server.stats.TimingStatistics;

import org.onosproject.drivers.server.impl.devices.DefaultBasicCpuCacheDevice;
import org.onosproject.drivers.server.impl.devices.DefaultCpuCacheHierarchyDevice;
import org.onosproject.drivers.server.impl.devices.DefaultCpuDevice;
import org.onosproject.drivers.server.impl.devices.DefaultMemoryHierarchyDevice;
import org.onosproject.drivers.server.impl.devices.DefaultMemoryModuleDevice;
import org.onosproject.drivers.server.impl.devices.DefaultNicDevice;
import org.onosproject.drivers.server.impl.devices.DefaultRestServerSBDevice;
import org.onosproject.drivers.server.impl.devices.DefaultServerDeviceDescription;
import org.onosproject.drivers.server.impl.stats.DefaultCpuStatistics;
import org.onosproject.drivers.server.impl.stats.DefaultMemoryStatistics;
import org.onosproject.drivers.server.impl.stats.DefaultMonitoringStatistics;
import org.onosproject.drivers.server.impl.stats.DefaultTimingStatistics;

import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.behaviour.DevicesDiscovery;
import org.onosproject.net.behaviour.DeviceCpuStats;
import org.onosproject.net.behaviour.DeviceMemoryStats;
import org.onosproject.net.behaviour.DeviceSystemStatisticsQuery;
import org.onosproject.net.behaviour.DeviceSystemStats;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.PortNumber;
import org.onosproject.protocol.rest.RestSBDevice;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.ProcessingException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_NAME_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_PORT_NUMBER_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_STATS_TIMING_DEPLOY_INCONSISTENT;
import static org.onosproject.drivers.server.Constants.PARAM_CAPACITY;
import static org.onosproject.drivers.server.Constants.PARAM_CHASSIS_ID;
import static org.onosproject.drivers.server.Constants.PARAM_CPUS;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_LEVEL;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_LEVELS;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_LINE_LEN;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_POLICY;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_SETS;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_SHARED;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_TYPE;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_WAYS;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHES;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CACHE_HIERARCHY;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_CORES;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_FREQUENCY;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_ID_LOG;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_ID_PHY;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_LOAD;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_LATENCY;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_QUEUE;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_SOCKET;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_SOCKETS;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_STATUS;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_THROUGHPUT;
import static org.onosproject.drivers.server.Constants.PARAM_CPU_VENDOR;
import static org.onosproject.drivers.server.Constants.PARAM_HW_VENDOR;
import static org.onosproject.drivers.server.Constants.PARAM_ID;
import static org.onosproject.drivers.server.Constants.PARAM_MANUFACTURER;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY_HIERARCHY;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY_MODULES;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY_STATS_FREE;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY_STATS_TOTAL;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY_STATS_USED;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY_WIDTH_DATA;
import static org.onosproject.drivers.server.Constants.PARAM_MEMORY_WIDTH_TOTAL;
import static org.onosproject.drivers.server.Constants.PARAM_NAME;
import static org.onosproject.drivers.server.Constants.PARAM_NICS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_HW_ADDR;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_PORT_TYPE;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_RX_FILTER;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_RX_COUNT;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_RX_BYTES;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_RX_DROPS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_RX_ERRORS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_TX_COUNT;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_TX_BYTES;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_TX_DROPS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_STATS_TX_ERRORS;
import static org.onosproject.drivers.server.Constants.PARAM_MON_AVERAGE;
import static org.onosproject.drivers.server.Constants.PARAM_MON_BUSY_CPUS;
import static org.onosproject.drivers.server.Constants.PARAM_MON_FREE_CPUS;
import static org.onosproject.drivers.server.Constants.PARAM_MON_MAX;
import static org.onosproject.drivers.server.Constants.PARAM_MON_MIN;
import static org.onosproject.drivers.server.Constants.PARAM_MON_UNIT;
import static org.onosproject.drivers.server.Constants.PARAM_SERIAL;
import static org.onosproject.drivers.server.Constants.PARAM_SPEED;
import static org.onosproject.drivers.server.Constants.PARAM_SPEED_CONF;
import static org.onosproject.drivers.server.Constants.PARAM_STATUS;
import static org.onosproject.drivers.server.Constants.PARAM_SW_VENDOR;
import static org.onosproject.drivers.server.Constants.PARAM_TIMING_AUTOSCALE;
import static org.onosproject.drivers.server.Constants.PARAM_TIMING_DEPLOY;
import static org.onosproject.drivers.server.Constants.PARAM_TIMING_LAUNCH;
import static org.onosproject.drivers.server.Constants.PARAM_TIMING_PARSE;
import static org.onosproject.drivers.server.Constants.PARAM_TIMING_STATS;
import static org.onosproject.drivers.server.Constants.PARAM_TIMING_STATS_AUTOSCALE;
import static org.onosproject.drivers.server.Constants.PARAM_TYPE;
import static org.onosproject.drivers.server.Constants.SLASH;
import static org.onosproject.drivers.server.Constants.URL_SERVICE_CHAINS_STATS;
import static org.onosproject.drivers.server.Constants.URL_SRV_GLOBAL_STATS;
import static org.onosproject.drivers.server.Constants.URL_SRV_RESOURCE_DISCOVERY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the device details of server devices.
 */
public class ServerDevicesDiscovery
        extends BasicServerDriver
        implements  DevicesDiscovery, DeviceDescriptionDiscovery,
                    PortStatisticsDiscovery, CpuStatisticsDiscovery,
                    MonitoringStatisticsDiscovery, DeviceSystemStatisticsQuery {

    private final Logger log = getLogger(getClass());

    /**
     * Auxiliary constants.
     */
    private static final short DISCOVERY_RETRIES = 3;

    /**
     * Constructs server device discovery.
     */
    public ServerDevicesDiscovery() {
        super();
        log.debug("Started");
    }

    /**
     * Implements DevicesDiscovery behaviour.
     */
    @Override
    public Set<DeviceId> deviceIds() {
        // Set of devices to return
        Set<DeviceId> devices = new HashSet<DeviceId>();

        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);
        devices.add(deviceId);

        return devices;
    }

    @Override
    public DeviceDescription deviceDetails(DeviceId deviceId) {
        return getDeviceDetails(deviceId);
    }

    /**
     * Implements DeviceDescriptionDiscovery behaviour.
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        return getDeviceDetails(null);
    }

    /**
     * Query a server to retrieve its features.
     *
     * @param deviceId the device ID to be queried
     * @return a DeviceDescription with the device's features
     */
    private DeviceDescription getDeviceDetails(DeviceId deviceId) {
        // Retrieve the device ID, if null given
        if (deviceId == null) {
            deviceId = getDeviceId();
            checkNotNull(deviceId, MSG_DEVICE_ID_NULL);
        }

        // Get the device
        RestSBDevice device = getDevice(deviceId);
        checkNotNull(device, MSG_DEVICE_NULL);

        // Hit the path that provides the server's resources
        InputStream response = null;
        try {
            response = getController().get(deviceId, URL_SRV_RESOURCE_DISCOVERY, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to discover the device details of: {}", deviceId);
            return null;
        }

        // Load the JSON into objects
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        JsonNode jsonNode = null;
        ObjectNode objNode = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("Failed to discover the device details of: {}", deviceId);
            return null;
        }

        if (jsonMap == null) {
            log.error("Failed to discover the device details of: {}", deviceId);
            return null;
        }

        // Get all the attributes
        String id      = get(jsonNode, PARAM_ID);
        String vendor  = get(jsonNode, PARAM_MANUFACTURER);
        String hw      = get(jsonNode, PARAM_HW_VENDOR);
        String sw      = get(jsonNode, PARAM_SW_VENDOR);
        String serial  = get(jsonNode, PARAM_SERIAL);
        long chassisId = objNode.path(PARAM_CHASSIS_ID).asLong();

        // Pass the southbound protocol as an annotation
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
        annotations.set(AnnotationKeys.PROTOCOL, "REST");

        // Parse CPU devices
        Collection<CpuDevice> cpuSet = parseCpuDevices(objNode);

        // Parse memory hierarchy device
        MemoryHierarchyDevice memHierarchyDev = parseMemoryHierarchyDevice(objNode);

        // Parse CPU cache hierachy device
        CpuCacheHierarchyDevice cacheHierarchyDev = parseCpuCacheHierarchyDevice(objNode);

        // NICs are composite attributes too
        Collection<NicDevice> nicSet = parseNicDevices(mapper, objNode, annotations);

        // Construct a server device,
        // i.e., a RestSBDevice extended with CPU, cache, memory, and NIC information
        RestServerSBDevice dev = new DefaultRestServerSBDevice(
            device.ip(), device.port(), device.username(),
            device.password(), device.protocol(), device.url(),
            device.isActive(), device.testUrl().orElse(""),
            vendor, hw, sw, cpuSet, cacheHierarchyDev,
            memHierarchyDev, nicSet);
        checkNotNull(dev, MSG_DEVICE_NULL);

        // Set alive
        raiseDeviceReconnect(dev);

        // Updates the controller with the complete device information
        getController().removeDevice(deviceId);
        getController().addDevice((RestSBDevice) dev);

        // Create a description for this server device
        ServerDeviceDescription desc = null;
        try {
            desc = new DefaultServerDeviceDescription(
                new URI(id), Device.Type.SERVER, vendor,
                hw, sw, serial, new ChassisId(chassisId),
                cpuSet, cacheHierarchyDev, memHierarchyDev,
                nicSet, annotations.build());
        } catch (URISyntaxException uEx) {
            log.error("Failed to create a server device description for: {}",
                deviceId);
            return null;
        }

        log.info("Device's {} details sent to the controller", deviceId);

        return desc;
    }

    /**
     * Parse the input JSON object, looking for CPU-related
     * information. Upon success, construct and return a list
     * of CPU devices.
     *
     * @param objNode input JSON node with CPU device information
     * @return list of CPU devices
     */
    private Collection<CpuDevice> parseCpuDevices(ObjectNode objNode) {
        Collection<CpuDevice> cpuSet = Sets.newHashSet();
        JsonNode cpuNode = objNode.path(PARAM_CPUS);

        // Construct CPU objects
        for (JsonNode cn : cpuNode) {
            ObjectNode cpuObjNode = (ObjectNode) cn;

            // All the CPU attributes
            int   physicalCpuId = cpuObjNode.path(PARAM_CPU_ID_PHY).asInt();
            int    logicalCpuId = cpuObjNode.path(PARAM_CPU_ID_LOG).asInt();
            int       cpuSocket = cpuObjNode.path(PARAM_CPU_SOCKET).asInt();
            String cpuVendorStr = get(cn, PARAM_CPU_VENDOR);
            long   cpuFrequency = cpuObjNode.path(PARAM_CPU_FREQUENCY).asLong();

            // Construct a CPU device and add it to the set
            cpuSet.add(
                DefaultCpuDevice.builder()
                    .setCoreId(logicalCpuId, physicalCpuId)
                    .setVendor(cpuVendorStr)
                    .setSocket(cpuSocket)
                    .setFrequency(cpuFrequency)
                    .build());
        }

        return cpuSet;
    }

    /**
     * Parse the input JSON object, looking for CPU cache-related
     * information. Upon success, construct and return a CPU cache
     * hierarchy device.
     *
     * @param objNode input JSON node with CPU cache device information
     * @return a CPU cache hierarchy devices
     */
    private CpuCacheHierarchyDevice parseCpuCacheHierarchyDevice(ObjectNode objNode) {
        JsonNode cacheHierarchyNode = objNode.path(PARAM_CPU_CACHE_HIERARCHY);
        ObjectNode cacheHierarchyObjNode = (ObjectNode) cacheHierarchyNode;
        if (cacheHierarchyObjNode == null) {
            return null;
        }

        int socketsNb = cacheHierarchyObjNode.path(PARAM_CPU_SOCKETS).asInt();
        int coresNb = cacheHierarchyObjNode.path(PARAM_CPU_CORES).asInt();
        int levels = cacheHierarchyObjNode.path(PARAM_CPU_CACHE_LEVELS).asInt();

        JsonNode cacheNode = cacheHierarchyObjNode.path(PARAM_CPU_CACHES);

        DefaultCpuCacheHierarchyDevice.Builder cacheBuilder =
            DefaultCpuCacheHierarchyDevice.builder()
                .setSocketsNumber(socketsNb)
                .setCoresNumber(coresNb)
                .setLevels(levels);

        // Construct CPU cache objects
        for (JsonNode cn : cacheNode) {
            ObjectNode cacheObjNode = (ObjectNode) cn;

            // CPU cache attributes
            String cpuVendorStr = get(cn, PARAM_CPU_VENDOR);
            String     levelStr = get(cn, PARAM_CPU_CACHE_LEVEL);
            String      typeStr = get(cn, PARAM_CPU_CACHE_TYPE);
            String    policyStr = get(cn, PARAM_CPU_CACHE_POLICY);
            long       capacity = cacheObjNode.path(PARAM_CAPACITY).asLong();
            int            sets = cacheObjNode.path(PARAM_CPU_CACHE_SETS).asInt();
            int            ways = cacheObjNode.path(PARAM_CPU_CACHE_WAYS).asInt();
            int         lineLen = cacheObjNode.path(PARAM_CPU_CACHE_LINE_LEN).asInt();
            boolean      shared = cacheObjNode.path(PARAM_CPU_CACHE_SHARED).asInt() > 0;

            // Construct a basic CPU cache device and add it to the hierarchy
            cacheBuilder.addBasicCpuCacheDevice(
                DefaultBasicCpuCacheDevice.builder()
                    .setVendor(cpuVendorStr)
                    .setCacheId(levelStr, typeStr)
                    .setPolicy(policyStr)
                    .setCapacity(capacity)
                    .setNumberOfSets(sets)
                    .setNumberOfWays(ways)
                    .setLineLength(lineLen)
                    .isShared(shared)
                    .build());
        }

        return cacheBuilder.build();
    }

    /**
     * Parse the input JSON object, looking for memory-related
     * information. Upon success, construct and return a memory
     * hierarchy device.
     *
     * @param objNode input JSON node with memory device information
     * @return a memory hierarchy device
     */
    private MemoryHierarchyDevice parseMemoryHierarchyDevice(ObjectNode objNode) {
        JsonNode memHierarchyNode = objNode.path(PARAM_MEMORY_HIERARCHY);
        ObjectNode memoryHierarchyObjNode = (ObjectNode) memHierarchyNode;
        if (memoryHierarchyObjNode == null) {
            return null;
        }

        JsonNode memoryNode = memoryHierarchyObjNode.path(PARAM_MEMORY_MODULES);

        DefaultMemoryHierarchyDevice.Builder memoryBuilder =
            DefaultMemoryHierarchyDevice.builder();

        // Construct memory modules
        for (JsonNode mn : memoryNode) {
            ObjectNode memoryObjNode = (ObjectNode) mn;

            String typeStr = get(mn, PARAM_TYPE);
            String manufacturerStr = get(mn, PARAM_MANUFACTURER);
            String serialStr = get(mn, PARAM_SERIAL);
            int dataWidth = memoryObjNode.path(PARAM_MEMORY_WIDTH_DATA).asInt();
            int totalWidth = memoryObjNode.path(PARAM_MEMORY_WIDTH_TOTAL).asInt();
            long capacity = memoryObjNode.path(PARAM_CAPACITY).asLong();
            long speed = memoryObjNode.path(PARAM_SPEED).asLong();
            long configuredSpeed = memoryObjNode.path(PARAM_SPEED_CONF).asLong();

            // Construct a memory module and add it to the hierarchy
            memoryBuilder.addMemoryModule(
                DefaultMemoryModuleDevice.builder()
                    .setType(typeStr)
                    .setManufacturer(manufacturerStr)
                    .setSerialNumber(serialStr)
                    .setDataWidth(dataWidth)
                    .setTotalWidth(totalWidth)
                    .setCapacity(capacity)
                    .setSpeed(speed)
                    .setConfiguredSpeed(configuredSpeed)
                    .build());
        }

        return memoryBuilder.build();
    }

    /**
     * Parse the input JSON object, looking for NIC-related
     * information. Upon success, construct and return a list
     * of NIC devices.
     *
     * @param mapper input JSON object mapper
     * @param objNode input JSON node with NIC device information
     * @param annotations device annotations
     * @return list of CPU devices
     */
    private Collection<NicDevice> parseNicDevices(
            ObjectMapper mapper, ObjectNode objNode, DefaultAnnotations.Builder annotations) {
        Collection<NicDevice> nicSet = Sets.newHashSet();
        JsonNode nicNode = objNode.path(PARAM_NICS);

        // Construct NIC objects
        for (JsonNode nn : nicNode) {
            ObjectNode nicObjNode = (ObjectNode) nn;

            // All the NIC attributes
            String nicName     = get(nn, PARAM_NAME);
            long nicIndex      = nicObjNode.path(PARAM_ID).asLong();
            long speed         = nicObjNode.path(PARAM_SPEED).asLong();
            String portTypeStr = get(nn, PARAM_NIC_PORT_TYPE);
            boolean status     = nicObjNode.path(PARAM_STATUS).asInt() > 0;
            String hwAddr      = get(nn, PARAM_NIC_HW_ADDR);
            JsonNode tagNode   = nicObjNode.path(PARAM_NIC_RX_FILTER);
            if (tagNode == null) {
                throw new IllegalArgumentException(
                    "The Rx filters of NIC " + nicName + " are not reported");
            }

            // Convert the JSON list into an array of strings
            List<String> rxFilters = null;
            try {
                rxFilters = mapper.readValue(tagNode.traverse(),
                    new TypeReference<ArrayList<String>>() { });
            } catch (IOException ioEx) {
                continue;
            }

            // Store NIC name to number mapping as an annotation
            annotations.set(nicName, Long.toString(nicIndex));

            // Construct a NIC device and add it to the set
            nicSet.add(
                DefaultNicDevice.builder()
                    .setName(nicName)
                    .setPortNumber(nicIndex)
                    .setPortNumber(nicIndex)
                    .setPortType(portTypeStr)
                    .setSpeed(speed)
                    .setStatus(status)
                    .setMacAddress(hwAddr)
                    .setRxFilters(rxFilters)
                    .build());
        }

        return nicSet;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        // Retrieve the device ID
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // .. and object
        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            log.error("Failed to discover ports for device {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        if (device == null) {
            log.error("No device with ID {} is available for port discovery", deviceId);
            return Collections.EMPTY_LIST;
        }
        if ((device.nics() == null) || (device.nics().size() == 0)) {
            log.error("No ports available on {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        // List of port descriptions to return
        List<PortDescription> portDescriptions = Lists.newArrayList();

        // Sorted list of NIC ports
        Set<NicDevice> nics = new TreeSet(device.nics());

        // Iterate through the NICs of this device to populate the list
        for (NicDevice nic : nics) {
            // Include the name of this device as an annotation
            DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                                .set(AnnotationKeys.PORT_NAME, nic.name());

            // Create a port description and add it to the list
            portDescriptions.add(
                    DefaultPortDescription.builder()
                            // CHECK: .withPortNumber(PortNumber.portNumber(nic.portNumber(), nic.name()))
                            .withPortNumber(PortNumber.portNumber(nic.portNumber()))
                            .isEnabled(nic.status())
                            .type(nic.portType())
                            .portSpeed(nic.speed())
                            .annotations(annotations.build())
                            .build());

            log.info("Port discovery on device {}: NIC {} is {} at {} Mbps",
                deviceId, nic.portNumber(), nic.status() ? "up" : "down",
                nic.speed());
        }

        return ImmutableList.copyOf(portDescriptions);
    }

    /**
     * Implements PortStatisticsDiscovery behaviour.
     */
    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        // Retrieve the device ID
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get port statistics for this device
        return getPortStatistics(deviceId);
    }

    /**
     * Query a server to retrieve its port statistics.
     *
     * @param deviceId the device ID to be queried
     * @return list of (per port) PortStatistics
     */
    private Collection<PortStatistics> getPortStatistics(DeviceId deviceId) {
        // Get global monitoring statistics
        MonitoringStatistics monStats = getGlobalMonitoringStatistics(deviceId);
        if (monStats == null) {
            return Collections.EMPTY_LIST;
        }

        // Filter out the NIC statistics
        Collection<PortStatistics> portStats = monStats.nicStatisticsAll();
        if (portStats == null) {
            return Collections.EMPTY_LIST;
        }

        log.debug("Port statistics: {}", portStats.toString());

        return portStats;
    }

    /**
     * Implements CpuStatisticsDiscovery behaviour.
     */
    @Override
    public Collection<CpuStatistics> discoverCpuStatistics() {
        // Retrieve the device ID
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get CPU statistics for this device
        return getCpuStatistics(deviceId);
    }

    /**
     * Query a server to retrieve its CPU statistics.
     *
     * @param deviceId the device ID to be queried
     * @return list of (per core) CpuStatistics
     */
     public Collection<CpuStatistics> getCpuStatistics(DeviceId deviceId) {
        // Get global monitoring statistics
        MonitoringStatistics monStats = getGlobalMonitoringStatistics(deviceId);
        if (monStats == null) {
            return Collections.EMPTY_LIST;
        }

        // Filter out the CPU statistics
        Collection<CpuStatistics> cpuStats = monStats.cpuStatisticsAll();
        if (cpuStats == null) {
            return Collections.EMPTY_LIST;
        }

        log.debug("CPU statistics: {}", cpuStats.toString());

        return cpuStats;
    }

    /**
     * Implements MonitoringStatisticsDiscovery behaviour.
     */
    @Override
    public MonitoringStatistics discoverGlobalMonitoringStatistics() {
        // Retrieve the device ID
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get global monitoring statistics for this device
        return getGlobalMonitoringStatistics(deviceId);
    }

    /**
     * Query a server to retrieve its global monitoring statistics.
     *
     * @param deviceId the device ID to be queried
     * @return global monitoring statistics
     */
     public MonitoringStatistics getGlobalMonitoringStatistics(DeviceId deviceId) {
        // Monitoring statistics to return
        MonitoringStatistics monStats = null;

        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            log.error("Failed to retrieve global monitoring statistics from device {}",
                deviceId);
            return monStats;
        }
        if ((device == null) || (!device.isActive())) {
            return monStats;
        }

        // Hit the path that provides the server's global resources
        InputStream response = null;
        try {
            response = getController().get(deviceId, URL_SRV_GLOBAL_STATS, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to retrieve global monitoring statistics from device {}",
                deviceId);
            raiseDeviceDisconnect(device);
            return monStats;
        }

        // Load the JSON into objects
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        ObjectNode objNode = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            JsonNode jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("Failed to retrieve global monitoring statistics from device {}",
                deviceId);
            raiseDeviceDisconnect(device);
            return monStats;
        }

        if (jsonMap == null) {
            log.error("Failed to retrieve global monitoring statistics from device {}",
                deviceId);
            raiseDeviceDisconnect(device);
            return monStats;
        }

        // Get high-level CPU statistics
        int busyCpus = objNode.path(PARAM_MON_BUSY_CPUS).asInt();
        int freeCpus = objNode.path(PARAM_MON_FREE_CPUS).asInt();

        // Get a list of CPU statistics per core
        Collection<CpuStatistics> cpuStats = parseCpuStatistics(deviceId, objNode);

        // Get main memory statistics
        MemoryStatistics memStats = parseMemoryStatistics(deviceId, objNode);

        // Get a list of port statistics
        Collection<PortStatistics> nicStats = parseNicStatistics(deviceId, objNode);

        // Get zero timing statistics
        TimingStatistics timinsgStats = getZeroTimingStatistics();

        // Construct a global monitoring statistics object out of smaller ones
        monStats = DefaultMonitoringStatistics.builder()
                    .setDeviceId(deviceId)
                    .setTimingStatistics(timinsgStats)
                    .setCpuStatistics(cpuStats)
                    .setMemoryStatistics(memStats)
                    .setNicStatistics(nicStats)
                    .build();

        // When a device reports monitoring data, it means it is alive
        raiseDeviceReconnect(device);

        log.debug("Global monitoring statistics: {}", monStats.toString());

        return monStats;
    }

    @Override
    public MonitoringStatistics discoverMonitoringStatistics(URI tcId) {
        // Retrieve the device ID
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get resource-specific monitoring statistics for this device
        return getMonitoringStatistics(deviceId, tcId);
    }

    /**
     * Query a server to retrieve monitoring statistics for a
     * specific resource (i.e., traffic class).
     *
     * @param deviceId the device ID to be queried
     * @param tcId the ID of the traffic class to be monitored
     * @return resource-specific monitoring statistics
     */
     private MonitoringStatistics getMonitoringStatistics(DeviceId deviceId, URI tcId) {
        // Monitoring statistics to return
        MonitoringStatistics monStats = null;

        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            log.error("Failed to retrieve monitoring statistics from device {}",
                deviceId);
            return monStats;
        }
        if (device == null) {
            return monStats;
        }

        // Create a resource-specific URL
        String scUrl = URL_SERVICE_CHAINS_STATS + SLASH + tcId.toString();

        // Hit the path that provides the server's specific resources
        InputStream response = null;
        try {
            response = getController().get(deviceId, scUrl, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to retrieve monitoring statistics from device {}",
                deviceId);
            raiseDeviceDisconnect(device);
            return monStats;
        }

        // Load the JSON into objects
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        JsonNode jsonNode  = null;
        ObjectNode objNode = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("Failed to retrieve monitoring statistics from device {}",
                deviceId);
            raiseDeviceDisconnect(device);
            return monStats;
        }

        if (jsonMap == null) {
            log.error("Failed to retrieve monitoring statistics from device {}",
                deviceId);
            raiseDeviceDisconnect(device);
            return monStats;
        }

        // Get the ID of the traffic class
        String id = get(jsonNode, PARAM_ID);

        // And verify that this is the traffic class we want to monitor
        if (!id.equals(tcId.toString())) {
            throw new IllegalStateException(
                "Failed to retrieve monitoring data for traffic class " + tcId +
                ". Traffic class ID does not agree."
            );
        }

        // Get a list of CPU statistics per core
        Collection<CpuStatistics> cpuStats = parseCpuStatistics(deviceId, objNode);

        // Get main memory statistics
        MemoryStatistics memStats = parseMemoryStatistics(deviceId, objNode);

        // Get a list of port statistics
        Collection<PortStatistics> nicStats = parseNicStatistics(deviceId, objNode);

        // Get timing statistics
        TimingStatistics timinsgStats = parseTimingStatistics(objNode);

        // Construct a global monitoring statistics object out of smaller ones
        monStats = DefaultMonitoringStatistics.builder()
                    .setDeviceId(deviceId)
                    .setTimingStatistics(timinsgStats)
                    .setCpuStatistics(cpuStats)
                    .setMemoryStatistics(memStats)
                    .setNicStatistics(nicStats)
                    .build();

        // When a device reports monitoring data, it means it is alive
        raiseDeviceReconnect(device);

        log.debug("Monitoring statistics: {}", monStats.toString());

        return monStats;
    }

    /**
     * Parse the input JSON object, looking for CPU-related
     * statistics. Upon success, construct and return a list
     * of CPU statistics objects.
     *
     * @param deviceId the device ID that sent the JSON object
     * @param objNode input JSON node with CPU statistics information
     * @return list of (per core) CpuStatistics
     */
    private Collection<CpuStatistics> parseCpuStatistics(DeviceId deviceId, JsonNode objNode) {
        if ((deviceId == null) || (objNode == null)) {
            return Collections.EMPTY_LIST;
        }

        Collection<CpuStatistics> cpuStats = Lists.newArrayList();

        JsonNode cpuNode = objNode.path(PARAM_CPUS);
        if (cpuNode.isMissingNode()) {
            return cpuStats;
        }

        for (JsonNode cn : cpuNode) {
            ObjectNode cpuObjNode = (ObjectNode) cn;

            // CPU statistics builder
            DefaultCpuStatistics.Builder cpuStatsBuilder = DefaultCpuStatistics.builder();

            // Throughput statistics are optional
            JsonNode throughputNode = cpuObjNode.get(PARAM_CPU_THROUGHPUT);
            if (throughputNode != null) {
                String throughputUnit = get(throughputNode, PARAM_MON_UNIT);
                if (!Strings.isNullOrEmpty(throughputUnit)) {
                    cpuStatsBuilder.setThroughputUnit(throughputUnit);
                }
                float averageThroughput = (float) 0;
                if (throughputNode.get(PARAM_MON_AVERAGE) != null) {
                    averageThroughput = throughputNode.path(PARAM_MON_AVERAGE).floatValue();
                }
                cpuStatsBuilder.setAverageThroughput(averageThroughput);
            }

            // Latency statistics are optional
            JsonNode latencyNode = cpuObjNode.get(PARAM_CPU_LATENCY);
            if (latencyNode != null) {
                String latencyUnit = get(latencyNode, PARAM_MON_UNIT);
                if (!Strings.isNullOrEmpty(latencyUnit)) {
                    cpuStatsBuilder.setLatencyUnit(latencyUnit);
                }
                float minLatency = (float) 0;
                if (latencyNode.get(PARAM_MON_MIN) != null) {
                    minLatency = latencyNode.path(PARAM_MON_MIN).floatValue();
                }
                float averageLatency = (float) 0;
                if (latencyNode.get(PARAM_MON_AVERAGE) != null) {
                    averageLatency = latencyNode.path(PARAM_MON_AVERAGE).floatValue();
                }
                float maxLatency = (float) 0;
                if (latencyNode.get(PARAM_MON_MAX) != null) {
                    maxLatency = latencyNode.path(PARAM_MON_MAX).floatValue();
                }

                cpuStatsBuilder.setMinLatency(minLatency)
                    .setAverageLatency(averageLatency)
                    .setMaxLatency(maxLatency);
            }

            // CPU ID with its load and status
            int cpuId = cpuObjNode.path(PARAM_ID).asInt();
            float cpuLoad = cpuObjNode.path(PARAM_CPU_LOAD).floatValue();
            int queueId = cpuObjNode.path(PARAM_CPU_QUEUE).asInt();
            int busySince = cpuObjNode.path(PARAM_CPU_STATUS).asInt();

            // We have all the statistics for this CPU core
            cpuStats.add(
                cpuStatsBuilder
                    .setDeviceId(deviceId)
                    .setId(cpuId)
                    .setLoad(cpuLoad)
                    .setQueue(queueId)
                    .setBusySince(busySince)
                    .build());
        }

        return cpuStats;
    }

    /**
     * Parse the input JSON object, looking for memory-related
     * statistics. Upon success, construct and return a memory
     * statistics objects.
     *
     * @param deviceId the device ID that sent the JSON object
     * @param objNode input JSON node with memory statistics information
     * @return memory statistics object
     */
    private MemoryStatistics parseMemoryStatistics(DeviceId deviceId, JsonNode objNode) {
        if ((deviceId == null) || (objNode == null)) {
            return null;
        }

        JsonNode memoryNode = objNode.path(PARAM_MEMORY);
        if (memoryNode.isMissingNode()) {
            return getZeroMemoryStatistics(deviceId);
        }
        ObjectNode memoryObjNode = (ObjectNode) memoryNode;

        // Fetch memory statistics
        String unit = get(memoryNode, PARAM_MON_UNIT);
        long used = memoryObjNode.path(PARAM_MEMORY_STATS_USED).asLong();
        long free = memoryObjNode.path(PARAM_MEMORY_STATS_FREE).asLong();
        long total = memoryObjNode.path(PARAM_MEMORY_STATS_TOTAL).asLong();

        // Memory statistics builder
        return DefaultMemoryStatistics.builder()
                .setDeviceId(deviceId)
                .setMemoryUsed(used)
                .setMemoryFree(free)
                .setMemoryTotal(total)
                .build();
    }

    /**
     * Parse the input JSON object, looking for NIC-related
     * statistics. Upon success, construct and return a list
     * of NIC statistics objects.
     *
     * @param deviceId the device ID that sent the JSON object
     * @param objNode input JSON node with NIC statistics information
     * @return list of (per port) PortStatistics
     */
    private Collection<PortStatistics> parseNicStatistics(DeviceId deviceId, JsonNode objNode) {
        if ((deviceId == null) || (objNode == null)) {
            return Collections.EMPTY_LIST;
        }

        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            return Collections.EMPTY_LIST;
        }
        if (device == null) {
            return Collections.EMPTY_LIST;
        }

        Collection<PortStatistics> nicStats = Lists.newArrayList();

        JsonNode nicNode = objNode.path(PARAM_NICS);
        if (nicNode.isMissingNode()) {
            return nicStats;
        }

        for (JsonNode nn : nicNode) {
            ObjectNode nicObjNode = (ObjectNode) nn;

            // All the NIC attributes
            String nicName  = get(nn, PARAM_NAME);
            checkArgument(!Strings.isNullOrEmpty(nicName), MSG_NIC_NAME_NULL);

            long portNumber = device.portNumberFromName(nicName);
            checkArgument(portNumber >= 0, MSG_NIC_PORT_NUMBER_NEGATIVE);

            long rxCount   = nicObjNode.path(PARAM_NIC_STATS_RX_COUNT).asLong();
            long rxBytes   = nicObjNode.path(PARAM_NIC_STATS_RX_BYTES).asLong();
            long rxDropped = nicObjNode.path(PARAM_NIC_STATS_RX_DROPS).asLong();
            long rxErrors  = nicObjNode.path(PARAM_NIC_STATS_RX_ERRORS).asLong();
            long txCount   = nicObjNode.path(PARAM_NIC_STATS_TX_COUNT).asLong();
            long txBytes   = nicObjNode.path(PARAM_NIC_STATS_TX_BYTES).asLong();
            long txDropped = nicObjNode.path(PARAM_NIC_STATS_TX_DROPS).asLong();
            long txErrors  = nicObjNode.path(PARAM_NIC_STATS_TX_ERRORS).asLong();

            // Construct a NIC statistics object and add it to the set
            nicStats.add(
                DefaultPortStatistics.builder()
                    .setDeviceId(deviceId)
                    .setPort(PortNumber.portNumber(portNumber))
                    .setPacketsReceived(rxCount)
                    .setPacketsSent(txCount)
                    .setBytesReceived(rxBytes)
                    .setBytesSent(txBytes)
                    .setPacketsRxDropped(rxDropped)
                    .setPacketsRxErrors(rxErrors)
                    .setPacketsTxDropped(txDropped)
                    .setPacketsTxErrors(txErrors)
                    .build());
        }

        return nicStats;
    }

    /**
     * Parse the input JSON object, looking for timing-related statistics.
     * Upon success, return a timing statistics object with the advertized values.
     * Upon failure, return a timing statistics object with zero-initialized values.
     *
     * @param objNode input JSON node with timing statistics information
     * @return TimingStatistics object or null
     */
    private TimingStatistics parseTimingStatistics(JsonNode objNode) {
        TimingStatistics timinsgStats = null;

        if (objNode == null) {
            return timinsgStats;
        }

        // Get timing statistics
        JsonNode timingNode = objNode.path(PARAM_TIMING_STATS);
        if (timingNode.isMissingNode()) {
            // If no timing statistics are present, then send zeros
            return getZeroTimingStatistics();
        }
        ObjectNode timingObjNode = (ObjectNode) timingNode;

        DefaultTimingStatistics.Builder timingBuilder =
            DefaultTimingStatistics.builder();

        // The unit of timing statistics
        String timingStatsUnit = get(timingNode, PARAM_MON_UNIT);
        if (!Strings.isNullOrEmpty(timingStatsUnit)) {
            timingBuilder.setUnit(timingStatsUnit);
        }

        // Time (ns) to parse the controller's deployment instruction
        long parsingTime = 0;
        if (timingObjNode.get(PARAM_TIMING_PARSE) != null) {
            parsingTime = timingObjNode.path(PARAM_TIMING_PARSE).asLong();
        }
        // Time (ns) to do the deployment
        long launchingTime = 0;
        if (timingObjNode.get(PARAM_TIMING_LAUNCH) != null) {
            launchingTime = timingObjNode.path(PARAM_TIMING_LAUNCH).asLong();
        }
        // Deployment time (ns) equals to time to parse + time to launch
        long deployTime = 0;
        if (timingObjNode.get(PARAM_TIMING_DEPLOY) != null) {
            deployTime = timingObjNode.path(PARAM_TIMING_DEPLOY).asLong();
        }
        checkArgument(deployTime == parsingTime + launchingTime,
            MSG_STATS_TIMING_DEPLOY_INCONSISTENT);

        timingBuilder.setParsingTime(parsingTime)
                    .setLaunchingTime(launchingTime);

        // Get autoscale timing statistics
        JsonNode autoscaleTimingNode = objNode.path(PARAM_TIMING_STATS_AUTOSCALE);
        if (autoscaleTimingNode == null) {
            return timingBuilder.build();
        }

        ObjectNode autoScaleTimingObjNode = (ObjectNode) autoscaleTimingNode;
        // Time (ns) to autoscale a server's load
        long autoScaleTime = 0;
        if (autoScaleTimingObjNode.get(PARAM_TIMING_AUTOSCALE) != null) {
            autoScaleTime = autoScaleTimingObjNode.path(PARAM_TIMING_AUTOSCALE).asLong();
        }
        timingBuilder.setAutoScaleTime(autoScaleTime);

        return timingBuilder.build();
    }

    /**
     * Return a memory statistics object with zero counters.
     * This is useful when constructing MonitoringStatistics
     * objects that do not require memory statistics.
     *
     * @param deviceId a device ID
     * @return MemoryStatistics object
     */
    private MemoryStatistics getZeroMemoryStatistics(DeviceId deviceId) {
        return DefaultMemoryStatistics.builder()
                    .setDeviceId(deviceId)
                    .setMemoryUsed(0)
                    .setMemoryFree(0)
                    .setMemoryTotal(0)
                    .build();
    }

    /**
     * Return a timing statistics object with zero counters.
     * This is useful when constructing MonitoringStatistics
     * objects that do not require timers.
     *
     * @return TimingStatistics object
     */
    private TimingStatistics getZeroTimingStatistics() {
        return DefaultTimingStatistics.builder()
                    .setParsingTime(0)
                    .setLaunchingTime(0)
                    .setAutoScaleTime(0)
                    .build();
    }

    /**
     * Implements DeviceSystemStatisticsQuery behaviour.
     */
    @Override
    public Optional<DeviceSystemStats> getDeviceSystemStats() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // ....to retrieve monitoring statistics
        MonitoringStatistics monStats = getGlobalMonitoringStatistics(deviceId);

        Optional<DeviceCpuStats> cpuStats = getOverallCpuUsage(monStats);
        Optional<DeviceMemoryStats> memoryStats = getOverallMemoryUsage(monStats);

        if (cpuStats.isPresent() && memoryStats.isPresent()) {
            return Optional.of(new DeviceSystemStats(memoryStats.get(), cpuStats.get()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get CPU usage of server device.
     *
     * @param monStats global monitoring statistics which contain CPU statistics
     * @return cpuStats, device CPU usage stats if available
     */
    private Optional<DeviceCpuStats> getOverallCpuUsage(MonitoringStatistics monStats) {
        if (monStats == null) {
            return Optional.empty();
        }

        if (monStats.numberOfCpus() == 0) {
            return Optional.of(new DeviceCpuStats());
        }

        float usedCpu = 0.0f;
        for (CpuStatistics cpuCoreStats : monStats.cpuStatisticsAll()) {
            if (cpuCoreStats.busy()) {
                usedCpu += cpuCoreStats.load();
            }
        }

        return Optional.of(new DeviceCpuStats(usedCpu / (float) monStats.numberOfCpus()));
    }

    /**
     * Get memory usage of server device in KB.
     *
     * @param monStats global monitoring statistics which contain memory statistics
     * @return memoryStats, device memory usage stats if available
     */
    private Optional<DeviceMemoryStats> getOverallMemoryUsage(MonitoringStatistics monStats) {
        if (monStats == null) {
            return Optional.empty();
        }

        MemoryStatistics memStats = monStats.memoryStatistics();

        return Optional.of(
            new DeviceMemoryStats(memStats.free(), memStats.used(), memStats.total()));
    }

}
