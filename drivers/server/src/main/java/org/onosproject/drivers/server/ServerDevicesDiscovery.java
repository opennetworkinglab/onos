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
import org.onosproject.drivers.server.devices.CpuDevice;
import org.onosproject.drivers.server.devices.CpuVendor;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.nic.NicRxFilter;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.devices.ServerDeviceDescription;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.drivers.server.stats.TimingStatistics;

import org.onosproject.drivers.server.impl.devices.DefaultCpuDevice;
import org.onosproject.drivers.server.impl.devices.DefaultNicDevice;
import org.onosproject.drivers.server.impl.devices.DefaultRestServerSBDevice;
import org.onosproject.drivers.server.impl.devices.DefaultServerDeviceDescription;
import org.onosproject.drivers.server.impl.stats.DefaultCpuStatistics;
import org.onosproject.drivers.server.impl.stats.DefaultMonitoringStatistics;
import org.onosproject.drivers.server.impl.stats.DefaultTimingStatistics;

import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.behaviour.DevicesDiscovery;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.protocol.rest.RestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice.AuthenticationScheme;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.ProcessingException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the device details of
 * REST-based commodity server devices.
 */
public class ServerDevicesDiscovery extends BasicServerDriver
        implements  DevicesDiscovery, DeviceDescriptionDiscovery,
                    PortStatisticsDiscovery, CpuStatisticsDiscovery,
                    MonitoringStatisticsDiscovery {

    private final Logger log = getLogger(getClass());

    /**
     * Resource endpoints of the server agent (REST server-side).
     */
    private static final String RESOURCE_DISCOVERY_URL   = BASE_URL + SLASH + "resources";
    private static final String GLOBAL_STATS_URL         = BASE_URL + SLASH + "stats";
    private static final String SERVICE_CHAINS_STATS_URL = BASE_URL + SLASH + "chains_stats";  // + /ID

    /**
     * Parameters to be exchanged with the server's agent.
     */
    private static final String PARAM_MANUFACTURER     = "manufacturer";
    private static final String PARAM_HW_VENDOR        = "hwVersion";
    private static final String PARAM_SW_VENDOR        = "swVersion";
    private static final String PARAM_SERIAL           = "serial";
    private static final String PARAM_TIMING_STATS     = "timingStats";
    private static final String PARAM_TIMING_AUTOSCALE = "autoScaleTimingStats";

    private static final String NIC_PARAM_NAME             = "name";
    private static final String NIC_PARAM_PORT_INDEX       = "index";
    private static final String NIC_PARAM_PORT_TYPE        = "portType";
    private static final String NIC_PARAM_PORT_TYPE_FIBER  = "fiber";
    private static final String NIC_PARAM_PORT_TYPE_COPPER = "copper";
    private static final String NIC_PARAM_SPEED            = "speed";
    private static final String NIC_PARAM_STATUS           = "status";
    private static final String NIC_PARAM_HW_ADDR          = "hwAddr";

    /**
     * NIC statistics.
     */
    private static final String NIC_STATS_TX_COUNT  = "txCount";
    private static final String NIC_STATS_TX_BYTES  = "txBytes";
    private static final String NIC_STATS_TX_DROPS  = "txDropped";
    private static final String NIC_STATS_TX_ERRORS = "txErrors";
    private static final String NIC_STATS_RX_COUNT  = "rxCount";
    private static final String NIC_STATS_RX_BYTES  = "rxBytes";
    private static final String NIC_STATS_RX_DROPS  = "rxDropped";
    private static final String NIC_STATS_RX_ERRORS = "rxErrors";

    /**
     * CPU statistics.
     */
    private static final String CPU_PARAM_ID         = "id";
    private static final String CPU_PARAM_VENDOR     = "vendor";
    private static final String CPU_PARAM_FREQUENCY  = "frequency";
    private static final String CPU_PARAM_LOAD       = "load";
    private static final String CPU_PARAM_QUEUE      = "queue";
    private static final String CPU_PARAM_STATUS     = "busy";
    private static final String CPU_PARAM_THROUGHPUT = "throughput";
    private static final String CPU_PARAM_LATENCY    = "latency";
    private static final String MON_PARAM_UNIT       = "unit";
    private static final String MON_PARAM_BUSY_CPUS  = "busyCpus";
    private static final String MON_PARAM_FREE_CPUS  = "freeCpus";
    private static final String MON_PARAM_MIN        = "min";
    private static final String MON_PARAM_AVERAGE    = "average";
    private static final String MON_PARAM_MAX        = "max";

    /**
     * Timing statistics.
     */
    private static final String TIMING_PARAM_PARSE     = "parseTime";
    private static final String TIMING_PARAM_LAUNCH    = "launchTime";
    private static final String TIMING_PARAM_DEPLOY    = "deployTime";
    private static final String TIMING_PARAM_AUTOSCALE = "autoScaleTime";

    /**
     * Auxiliary constants.
     */
    private static final short  DISCOVERY_RETRIES  = 3;
    private static final String CPU_VENDOR_NULL    = "Unsupported CPU vendor" +
        " Choose one in: " + BasicServerDriver.enumTypesToString(CpuVendor.class);
    private static final String NIC_RX_FILTER_NULL = "Unsupported NIC Rx filter" +
        " Choose one in: " + BasicServerDriver.enumTypesToString(RxFilter.class);

    /**
     * Port types that usually appear in commodity servers.
     */
    public static final Map<String, Port.Type> PORT_TYPE_MAP =
        Collections.unmodifiableMap(
            new HashMap<String, Port.Type>() {
                {
                    put(NIC_PARAM_PORT_TYPE_FIBER,  Port.Type.FIBER);
                    put(NIC_PARAM_PORT_TYPE_COPPER, Port.Type.COPPER);
                }
            }
        );

    /**
     * Constructs server device discovery.
     */
    public ServerDevicesDiscovery() {
        super();
        log.debug("Started");
    }

    @Override
    public Set<DeviceId> deviceIds() {
        // Set of devices to return
        Set<DeviceId> devices = new HashSet<DeviceId>();

        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);
        devices.add(deviceId);

        return devices;
    }

    @Override
    public DeviceDescription deviceDetails(DeviceId deviceId) {
        return getDeviceDetails(deviceId);
    }

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
            deviceId = getHandler().data().deviceId();
            checkNotNull(deviceId, DEVICE_ID_NULL);
        }

        // Get the device
        RestSBDevice device = getController().getDevice(deviceId);
        checkNotNull(device, DEVICE_NULL);

        // Hit the path that provides the server's resources
        InputStream response = null;
        try {
            response = getController().get(deviceId, RESOURCE_DISCOVERY_URL, JSON);
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
        String id     = get(jsonNode, BasicServerDriver.PARAM_ID);
        String vendor = get(jsonNode, PARAM_MANUFACTURER);
        String hw     = get(jsonNode, PARAM_HW_VENDOR);
        String sw     = get(jsonNode, PARAM_SW_VENDOR);
        String serial = get(jsonNode, PARAM_SERIAL);

        // CPUs are composite attributes
        Set<CpuDevice> cpuSet = new HashSet<CpuDevice>();
        JsonNode cpuNode = objNode.path(BasicServerDriver.PARAM_CPUS);

        // Construct CPU objects
        for (JsonNode cn : cpuNode) {
            ObjectNode cpuObjNode = (ObjectNode) cn;

            // All the CPU attributes
            int           cpuId = cpuObjNode.path(CPU_PARAM_ID).asInt();
            String cpuVendorStr = get(cn, CPU_PARAM_VENDOR);
            long   cpuFrequency = cpuObjNode.path(CPU_PARAM_FREQUENCY).asLong();

            // Verify that this is a valid vendor
            CpuVendor cpuVendor = CpuVendor.getByName(cpuVendorStr);
            checkNotNull(cpuVendor, CPU_VENDOR_NULL);

            // Construct a CPU device
            CpuDevice cpu = new DefaultCpuDevice(cpuId, cpuVendor, cpuFrequency);

            // Add it to the set
            cpuSet.add(cpu);
        }

        // NICs are composite attributes too
        Set<NicDevice> nicSet = new HashSet<NicDevice>();
        JsonNode nicNode = objNode.path(PARAM_NICS);

        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();

        // Pass the southbound protocol as an annotation
        annotations.set(AnnotationKeys.PROTOCOL, "REST");

        // Construct NIC objects
        for (JsonNode nn : nicNode) {
            ObjectNode nicObjNode = (ObjectNode) nn;

            // All the NIC attributes
            String nicName     = get(nn, NIC_PARAM_NAME);
            long nicIndex      = nicObjNode.path(NIC_PARAM_PORT_INDEX).asLong();
            long speed         = nicObjNode.path(NIC_PARAM_SPEED).asLong();
            String portTypeStr = get(nn, NIC_PARAM_PORT_TYPE);
            Port.Type portType = PORT_TYPE_MAP.get(portTypeStr);
            if (portType == null) {
                throw new IllegalArgumentException(
                    portTypeStr + " is not a valid port type for NIC " + nicName);
            }
            boolean status     = nicObjNode.path(NIC_PARAM_STATUS).asInt() > 0;
            String hwAddr      = get(nn, NIC_PARAM_HW_ADDR);
            JsonNode tagNode   = nicObjNode.path(BasicServerDriver.NIC_PARAM_RX_FILTER);
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

            // Parse the array of strings and create an RxFilter object
            NicRxFilter rxFilterMechanism = new NicRxFilter();
            for (String s : rxFilters) {
                // Verify that this is a valid Rx filter
                RxFilter rf = RxFilter.getByName(s);
                checkNotNull(rf, NIC_RX_FILTER_NULL);

                rxFilterMechanism.addRxFilter(rf);
            }

            // Store NIC name to number mapping as an annotation
            annotations.set(nicName, Long.toString(nicIndex));

            // Construct a NIC device for this server
            NicDevice nic = new DefaultNicDevice(
                nicName, nicIndex, portType, speed, status, hwAddr, rxFilterMechanism);

            // Add it to the set
            nicSet.add(nic);
        }

        // Construct a complete server device object.
        // Lists of NICs and CPUs extend the information
        // already in RestSBDevice (parent class).
        RestServerSBDevice dev = new DefaultRestServerSBDevice(
            device.ip(), device.port(), device.username(),
            device.password(), device.protocol(), device.url(),
            device.isActive(), device.testUrl().orElse(""),
            vendor, hw, sw, AuthenticationScheme.BASIC, "",
            cpuSet, nicSet
        );
        checkNotNull(dev, DEVICE_NULL);

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
                hw, sw, serial, new ChassisId(),
                cpuSet, nicSet, annotations.build()
            );
        } catch (URISyntaxException uEx) {
            log.error("Failed to create a server device description for: {}",
                deviceId);
            return null;
        }

        log.info("Device's {} details sent to the controller", deviceId);

        return desc;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        // Retrieve the device ID
        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

        // .. and object
        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) getController().getDevice(deviceId);
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
                            .withPortNumber(PortNumber.portNumber(nic.portNumber(), nic.name()))
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

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        // Retrieve the device ID
        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

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

    @Override
    public Collection<CpuStatistics> discoverCpuStatistics() {
        // Retrieve the device ID
        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

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

    @Override
    public MonitoringStatistics discoverGlobalMonitoringStatistics() {
        // Retrieve the device ID
        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

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
            device = (RestServerSBDevice) getController().getDevice(deviceId);
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
            response = getController().get(deviceId, GLOBAL_STATS_URL, JSON);
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
        int busyCpus = objNode.path(MON_PARAM_BUSY_CPUS).asInt();
        int freeCpus = objNode.path(MON_PARAM_FREE_CPUS).asInt();

        // Get a list of CPU statistics per core
        Collection<CpuStatistics> cpuStats = parseCpuStatistics(deviceId, objNode);

        // Get a list of port statistics
        Collection<PortStatistics> nicStats = parseNicStatistics(deviceId, objNode);

        // Get zero timing statistics
        TimingStatistics timinsgStats = getZeroTimingStatistics();

        // Ready to construct the grand object
        DefaultMonitoringStatistics.Builder statsBuilder =
            DefaultMonitoringStatistics.builder();

        statsBuilder.setDeviceId(deviceId)
                .setTimingStatistics(timinsgStats)
                .setCpuStatistics(cpuStats)
                .setNicStatistics(nicStats);

        monStats = statsBuilder.build();

        // When a device reports monitoring data, it means it is alive
        raiseDeviceReconnect(device);

        log.debug("Global monitoring statistics: {}", monStats.toString());

        return monStats;
    }

    @Override
    public MonitoringStatistics discoverMonitoringStatistics(URI tcId) {
        // Retrieve the device ID
        DeviceId deviceId = getHandler().data().deviceId();
        checkNotNull(deviceId, DEVICE_ID_NULL);

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
            device = (RestServerSBDevice) getController().getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            log.error("Failed to retrieve monitoring statistics from device {}",
                deviceId);
            return monStats;
        }
        if (device == null) {
            return monStats;
        }

        // Create a resource-specific URL
        String scUrl = SERVICE_CHAINS_STATS_URL + SLASH + tcId.toString();

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

        // Get a list of port statistics
        Collection<PortStatistics> nicStats = parseNicStatistics(deviceId, objNode);

        // Get timing statistics
        TimingStatistics timinsgStats = parseTimingStatistics(objNode);

        // Ready to construct the grand object
        DefaultMonitoringStatistics.Builder statsBuilder =
            DefaultMonitoringStatistics.builder();

        statsBuilder.setDeviceId(deviceId)
                .setTimingStatistics(timinsgStats)
                .setCpuStatistics(cpuStats)
                .setNicStatistics(nicStats);

        monStats = statsBuilder.build();

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

        JsonNode cpuNode = objNode.path(BasicServerDriver.PARAM_CPUS);

        for (JsonNode cn : cpuNode) {
            ObjectNode cpuObjNode = (ObjectNode) cn;

            // CPU statistics builder
            DefaultCpuStatistics.Builder cpuBuilder = DefaultCpuStatistics.builder();

            // Throughput statistics are optional
            JsonNode throughputNode = cpuObjNode.get(CPU_PARAM_THROUGHPUT);
            if (throughputNode != null) {
                String throughputUnit = get(throughputNode, MON_PARAM_UNIT);
                if (!Strings.isNullOrEmpty(throughputUnit)) {
                    cpuBuilder.setThroughputUnit(throughputUnit);
                }
                float averageThroughput = (float) 0;
                if (throughputNode.get(MON_PARAM_AVERAGE) != null) {
                    averageThroughput = throughputNode.path(MON_PARAM_AVERAGE).floatValue();
                }
                cpuBuilder.setAverageThroughput(averageThroughput);
            }

            // Latency statistics are optional
            JsonNode latencyNode = cpuObjNode.get(CPU_PARAM_LATENCY);
            if (latencyNode != null) {
                String latencyUnit = get(latencyNode, MON_PARAM_UNIT);
                if (!Strings.isNullOrEmpty(latencyUnit)) {
                    cpuBuilder.setLatencyUnit(latencyUnit);
                }
                float minLatency = (float) 0;
                if (latencyNode.get(MON_PARAM_MIN) != null) {
                    minLatency = latencyNode.path(MON_PARAM_MIN).floatValue();
                }
                float averageLatency = (float) 0;
                if (latencyNode.get(MON_PARAM_AVERAGE) != null) {
                    averageLatency = latencyNode.path(MON_PARAM_AVERAGE).floatValue();
                }
                float maxLatency = (float) 0;
                if (latencyNode.get(MON_PARAM_MAX) != null) {
                    maxLatency = latencyNode.path(MON_PARAM_MAX).floatValue();
                }

                cpuBuilder.setMinLatency(minLatency)
                    .setAverageLatency(averageLatency)
                    .setMaxLatency(maxLatency);
            }

            // CPU ID with its load and status
            int cpuId = cpuObjNode.path(CPU_PARAM_ID).asInt();
            float cpuLoad = cpuObjNode.path(CPU_PARAM_LOAD).floatValue();
            int queueId = cpuObjNode.path(CPU_PARAM_QUEUE).asInt();
            int busySince = cpuObjNode.path(CPU_PARAM_STATUS).asInt();

            // This is mandatory information
            cpuBuilder.setDeviceId(deviceId)
                    .setId(cpuId)
                    .setLoad(cpuLoad)
                    .setQueue(queueId)
                    .setBusySince(busySince);

            // We have all the statistics for this CPU core
            cpuStats.add(cpuBuilder.build());
        }

        return cpuStats;
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
            device = (RestServerSBDevice) getController().getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            return Collections.EMPTY_LIST;
        }
        if (device == null) {
            return Collections.EMPTY_LIST;
        }

        Collection<PortStatistics> nicStats = Lists.newArrayList();

        JsonNode nicNode = objNode.path(PARAM_NICS);

        for (JsonNode nn : nicNode) {
            ObjectNode nicObjNode = (ObjectNode) nn;

            // All the NIC attributes
            String nicName  = get(nn, NIC_PARAM_NAME);
            checkArgument(!Strings.isNullOrEmpty(nicName), "NIC name is empty or NULL");

            long portNumber = device.portNumberFromName(nicName);
            checkArgument(portNumber >= 0, "Unknown port ID " + portNumber + " for NIC " + nicName);

            long rxCount   = nicObjNode.path(NIC_STATS_RX_COUNT).asLong();
            long rxBytes   = nicObjNode.path(NIC_STATS_RX_BYTES).asLong();
            long rxDropped = nicObjNode.path(NIC_STATS_RX_DROPS).asLong();
            long rxErrors  = nicObjNode.path(NIC_STATS_RX_ERRORS).asLong();
            long txCount   = nicObjNode.path(NIC_STATS_TX_COUNT).asLong();
            long txBytes   = nicObjNode.path(NIC_STATS_TX_BYTES).asLong();
            long txDropped = nicObjNode.path(NIC_STATS_TX_DROPS).asLong();
            long txErrors  = nicObjNode.path(NIC_STATS_TX_ERRORS).asLong();

            // Incorporate these statistics into an object
            DefaultPortStatistics.Builder nicBuilder = DefaultPortStatistics.builder();

            nicBuilder.setDeviceId(deviceId)
                    .setPort(PortNumber.portNumber(portNumber))
                    .setPacketsReceived(rxCount)
                    .setPacketsSent(txCount)
                    .setBytesReceived(rxBytes)
                    .setBytesSent(txBytes)
                    .setPacketsRxDropped(rxDropped)
                    .setPacketsRxErrors(rxErrors)
                    .setPacketsTxDropped(txDropped)
                    .setPacketsTxErrors(txErrors);

            // We have statistics for this NIC
            nicStats.add(nicBuilder.build());
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

        // If no timing statistics are present, then send zeros
        if (objNode.get(PARAM_TIMING_STATS) == null) {
            return getZeroTimingStatistics();
        }

        DefaultTimingStatistics.Builder timingBuilder = DefaultTimingStatistics.builder();

        // Get timing statistics
        JsonNode timingNode = objNode.path(PARAM_TIMING_STATS);
        ObjectNode timingObjNode = (ObjectNode) timingNode;

        // The unit of timing statistics
        String timingStatsUnit = get(timingNode, MON_PARAM_UNIT);
        if (!Strings.isNullOrEmpty(timingStatsUnit)) {
            timingBuilder.setUnit(timingStatsUnit);
        }

        // Time (ns) to parse the controller's deployment instruction
        long parsingTime = 0;
        if (timingObjNode.get(TIMING_PARAM_PARSE) != null) {
            parsingTime = timingObjNode.path(TIMING_PARAM_PARSE).asLong();
        }
        // Time (ns) to do the deployment
        long launchingTime = 0;
        if (timingObjNode.get(TIMING_PARAM_LAUNCH) != null) {
            launchingTime = timingObjNode.path(TIMING_PARAM_LAUNCH).asLong();
        }
        // Deployment time (ns) equals to time to parse + time to launch
        long deployTime = 0;
        if (timingObjNode.get(TIMING_PARAM_DEPLOY) != null) {
            deployTime = timingObjNode.path(TIMING_PARAM_DEPLOY).asLong();
        }
        checkArgument(deployTime == parsingTime + launchingTime, "Inconsistent timing statistics");

        timingBuilder.setParsingTime(parsingTime)
                    .setLaunchingTime(launchingTime);

        // Get autoscale timing statistics
        JsonNode autoscaleTimingNode = objNode.path(PARAM_TIMING_AUTOSCALE);
        if (autoscaleTimingNode == null) {
            return timingBuilder.build();
        }

        ObjectNode autoScaleTimingObjNode = (ObjectNode) autoscaleTimingNode;
        // Time (ns) to autoscale a server's load
        long autoScaleTime = 0;
        if (autoScaleTimingObjNode.get(TIMING_PARAM_AUTOSCALE) != null) {
            autoScaleTime = autoScaleTimingObjNode.path(TIMING_PARAM_AUTOSCALE).asLong();
        }
        timingBuilder.setAutoScaleTime(autoScaleTime);

        return timingBuilder.build();
    }

    /**
     * Return a timing statistics object with zero counters.
     * This is useful when constructing MonitoringStatistics
     * objects that do not require timers.
     *
     * @return TimingStatistics object
     */
    private TimingStatistics getZeroTimingStatistics() {
        DefaultTimingStatistics.Builder zeroTimingBuilder = DefaultTimingStatistics.builder();

        zeroTimingBuilder.setParsingTime(0)
                         .setLaunchingTime(0)
                         .setAutoScaleTime(0);

        return zeroTimingBuilder.build();
    }

}
