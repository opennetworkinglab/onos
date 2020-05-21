/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onosproject.drivers.server.devices.cpu.CpuCoreId;
import org.onosproject.drivers.server.devices.cpu.CpuDevice;
import org.onosproject.drivers.server.devices.cpu.CpuVendor;
import org.onosproject.drivers.server.devices.memory.MemoryModuleDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.drivers.server.stats.CpuStatistics;

import javax.ws.rs.core.MediaType;

/**
 * Provides constants used by the server device driver.
 */
public final class Constants {

    private Constants() {

    }

    /**
     * Generic parameters to be exchanged with the server's agent.
     */
    public static final String PARAM_AUTOSCALE              = "autoScale";
    public static final String PARAM_CAPACITY               = "capacity";
    public static final String PARAM_CHASSIS_ID             = "chassisId";
    public static final String PARAM_CONNECTION_STATUS      = "connectionStatus";
    public static final String PARAM_CPU_CACHES             = "cpuCaches";
    public static final String PARAM_CPU_CACHE_HIERARCHY    = "cpuCacheHierarchy";
    public static final String PARAM_CPUS                   = "cpus";
    public static final String PARAM_HW_VENDOR              = "hwVersion";
    public static final String PARAM_ID                     = "id";
    public static final String PARAM_MANUFACTURER           = "manufacturer";
    public static final String PARAM_MEMORY                 = "memory";
    public static final String PARAM_MEMORY_HIERARCHY       = "memoryHierarchy";
    public static final String PARAM_MEMORY_MODULES         = "modules";
    public static final String PARAM_NAME                   = "name";
    public static final String PARAM_NICS                   = "nics";
    public static final String PARAM_QUEUES                 = "queues";
    public static final String PARAM_RULES                  = "rules";
    public static final String PARAM_RULE_CONTENT           = "content";
    public static final String PARAM_SPEED                  = "speed";
    public static final String PARAM_SPEED_CONF             = "speedConfigured";
    public static final String PARAM_SW_VENDOR              = "swVersion";
    public static final String PARAM_SERIAL                 = "serial";
    public static final String PARAM_STATUS                 = "status";
    public static final String PARAM_TIME                   = "time";
    public static final String PARAM_TIMING_STATS           = "timingStats";
    public static final String PARAM_TIMING_STATS_AUTOSCALE = "timingStatsAutoscale";
    public static final String PARAM_TYPE                   = "type";

    /**
     * Controller configuration parameters.
     */
    public static final String PARAM_CTRL      = "controllers";
    public static final String PARAM_CTRL_IP   = "ip";
    public static final String PARAM_CTRL_PORT = "port";
    public static final String PARAM_CTRL_TYPE = "type";

    /**
     * CPU parameters.
     */
    public static final String PARAM_CPU_CORES   = "cores";
    public static final String PARAM_CPU_ID_LOG  = "logicalId";
    public static final String PARAM_CPU_ID_PHY  = "physicalId";
    public static final String PARAM_CPU_SOCKET  = "socket";
    public static final String PARAM_CPU_SOCKETS = "sockets";
    public static final String PARAM_CPU_VENDOR  = "vendor";
    public static final String PARAM_CPUS_MAX    = "maxCpus";

    /**
     * CPU cache parameters.
     */
    public static final String PARAM_CPU_CACHE_LEVEL    = "level";
    public static final String PARAM_CPU_CACHE_LEVELS   = "levels";
    public static final String PARAM_CPU_CACHE_SHARED   = "shared";
    public static final String PARAM_CPU_CACHE_LINE_LEN = "lineLength";
    public static final String PARAM_CPU_CACHE_POLICY   = "policy";
    public static final String PARAM_CPU_CACHE_SETS     = "sets";
    public static final String PARAM_CPU_CACHE_TYPE     = "type";
    public static final String PARAM_CPU_CACHE_WAYS     = "ways";

    /**
     * Memory parameters.
     */
    public static final String PARAM_MEMORY_STATS_FREE  = "free";
    public static final String PARAM_MEMORY_STATS_TOTAL = "total";
    public static final String PARAM_MEMORY_STATS_USED  = "used";
    public static final String PARAM_MEMORY_WIDTH_DATA  = "dataWidth";
    public static final String PARAM_MEMORY_WIDTH_TOTAL = "totalWidth";

    /**
     * NIC parameters.
     */
    public static final String PARAM_NIC_HW_ADDR              = "hwAddr";
    public static final String PARAM_NIC_MAX_RATE             = "maxRate";
    public static final String PARAM_NIC_PORT                 = "port";
    public static final String PARAM_NIC_PORT_TYPE            = "portType";
    public static final String PARAM_NIC_PORT_TYPE_COPPER     = "copper";
    public static final String PARAM_NIC_PORT_TYPE_FIBER      = "fiber";
    public static final String PARAM_NIC_PORT_STATUS          = "portStatus";
    public static final String PARAM_NIC_RX_FILTER            = "rxFilter";
    public static final String PARAM_NIC_RX_FILTER_FD         = "flow";
    /* Rx filtering methods usually implemented by NICs in commodity servers. */
    public static final String PARAM_NIC_RX_METHOD            = "method";
    public static final String PARAM_NIC_RX_METHOD_FLOW       = "flow";
    public static final String PARAM_NIC_RX_METHOD_MAC        = "mac";
    public static final String PARAM_NIC_RX_METHOD_MPLS       = "mpls";
    public static final String PARAM_NIC_RX_METHOD_RSS        = "rss";
    public static final String PARAM_NIC_RX_METHOD_VLAN       = "vlan";
    public static final String PARAM_NIC_RX_METHOD_VALUES     = "values";
    public static final String PARAM_NIC_TABLE                = "table";
    public static final String PARAM_NIC_TABLE_ACTIVE_ENTRIES = "activeEntries";
    public static final String PARAM_NIC_TABLE_PKTS_LOOKED_UP = "pktsLookedUp";
    public static final String PARAM_NIC_TABLE_PKTS_MATCHED   = "pktsMatched";
    public static final String PARAM_NIC_TABLE_MAX_SIZE       = "maxSize";

    /**
     * NIC statistics' parameters.
     */
    public static final String PARAM_NIC_STATS_RX_COUNT  = "rxCount";
    public static final String PARAM_NIC_STATS_RX_BYTES  = "rxBytes";
    public static final String PARAM_NIC_STATS_RX_DROPS  = "rxDropped";
    public static final String PARAM_NIC_STATS_RX_ERRORS = "rxErrors";
    public static final String PARAM_NIC_STATS_TX_COUNT  = "txCount";
    public static final String PARAM_NIC_STATS_TX_BYTES  = "txBytes";
    public static final String PARAM_NIC_STATS_TX_DROPS  = "txDropped";
    public static final String PARAM_NIC_STATS_TX_ERRORS = "txErrors";

    /**
     * CPU statistics' parameters.
     */
    public static final String PARAM_CPU_FREQUENCY  = "frequency";
    public static final String PARAM_CPU_LATENCY    = "latency";
    public static final String PARAM_CPU_LOAD       = "load";
    public static final String PARAM_CPU_QUEUE      = "queue";
    public static final String PARAM_CPU_STATUS     = "busy";
    public static final String PARAM_CPU_THROUGHPUT = "throughput";

    /**
     * Other monitoring statistics' parameters.
     */
    public static final String PARAM_MON_AVERAGE    = "average";
    public static final String PARAM_MON_BUSY_CPUS  = "busyCpus";
    public static final String PARAM_MON_FREE_CPUS  = "freeCpus";
    public static final String PARAM_MON_MAX        = "max";
    public static final String PARAM_MON_MIN        = "min";
    public static final String PARAM_MON_UNIT       = "unit";

    /**
     * Timing statistics' parameters.
     */
    public static final String PARAM_TIMING_AUTOSCALE = "autoScaleTime";
    public static final String PARAM_TIMING_DEPLOY    = "deployTime";
    public static final String PARAM_TIMING_LAUNCH    = "launchTime";
    public static final String PARAM_TIMING_PARSE     = "parseTime";

    /**
     * Resource API endpoints.
     */
    public static final MediaType JSON = MediaType.valueOf(MediaType.APPLICATION_JSON);
    public static final String SLASH = "/";
    public static final String URL_ROOT = "";
    public static final String URL_BASE = URL_ROOT + SLASH + "metron";

    public static final String URL_CONTROLLERS_GET        = URL_BASE + SLASH + "controllers";
    public static final String URL_CONTROLLERS_SET        = URL_BASE + SLASH + "controllers";
    public static final String URL_CONTROLLERS_DEL        = URL_BASE + SLASH + "controllers_delete";
    public static final String URL_NIC_LINK_DISCOVERY     = URL_BASE + SLASH + "nic_link_disc";
    public static final String URL_NIC_PORT_ADMIN         = URL_BASE + SLASH + "nic_ports";
    public static final String URL_NIC_QUEUE_ADMIN        = URL_BASE + SLASH + "nic_queues";
    public static final String URL_RULE_MANAGEMENT        = URL_BASE + SLASH + "rules";
    public static final String URL_RULE_TABLE_STATS       = URL_BASE + SLASH + "rules_table_stats";
    public static final String URL_SERVICE_CHAINS_STATS   = URL_BASE + SLASH + "service_chains_stats";  // + /ID
    public static final String URL_SRV_GLOBAL_STATS       = URL_BASE + SLASH + "server_stats";
    public static final String URL_SRV_PROBE_CONNECT      = URL_BASE + SLASH + "server_connect";
    public static final String URL_SRV_PROBE_DISCONNECT   = URL_BASE + SLASH + "server_disconnect";
    public static final String URL_SRV_RESOURCE_DISCOVERY = URL_BASE + SLASH + "server_resources";
    public static final String URL_SRV_TIME_DISCOVERY     = URL_BASE + SLASH + "server_time";

    /**
     * Messages for error handlers.
     */
    public static final String MSG_CONTROLLER_NULL = "RestSB controller is NULL";
    public static final String MSG_DEVICE_NULL     = "Device cannot be NULL";
    public static final String MSG_DEVICE_ID_NULL  = "Device ID cannot be NULL";
    public static final String MSG_HANDLER_NULL    = "Handler cannot be NULL";
    public static final String MSG_MASTERSHIP_NULL = "Mastership service is NULL";
    public static final String MSG_RESPONSE_NULL   = "Server's response is NULL";

    public static final String MSG_CPU_CACHE_CAPACITY_NEGATIVE =
        "CPU cache capacity (in kilo bytes) must be a positive integer";
    public static final String MSG_CPU_CACHE_CAPACITY_CORE_NEGATIVE = "Per core CPU cache capacity must be positive";
    public static final String MSG_CPU_CACHE_CAPACITY_LLC_NEGATIVE = "LLC capacity must be positive";
    public static final String MSG_CPU_CACHE_CAPACITY_TOTAL_NEGATIVE = "Total CPU cache capacity must be positive";
    public static final String MSG_CPU_CACHE_HIERARCHY_NULL = "CPU cache hierarchy cannot be NULL";
    public static final String MSG_CPU_CACHE_ID_NULL = "CPU cache ID cannot be NULL";
    public static final String MSG_CPU_CACHE_INSERTION_FAILED = "Failed to insert basic CPU cache into the hierarchy";
    public static final String MSG_CPU_CACHE_LEVELS_EXCEEDED =
        "Exceeded the typical number of levels of a CPU cache hierarchy";
    public static final String MSG_CPU_CACHE_LEVEL_NULL = "CPU cache level cannot be NULL";
    public static final String MSG_CPU_CACHE_LEVELS_NEGATIVE =
        "Number of CPU cache levels must be positive";
    public static final String MSG_CPU_CACHE_LINE_NEGATIVE =
        "CPU cache line length (in bytes) must be a positive integer";
    public static final String MSG_CPU_CACHE_POLICY_NULL = "PU cache policy cannot be NULL";
    public static final String MSG_CPU_CACHE_SETS_NEGATIVE = "CPU cache sets must be a positive integer";
    public static final String MSG_CPU_CACHE_TYPE_NULL = "CPU cache type cannot be NULL";
    public static final String MSG_CPU_CACHE_WAYS_NEGATIVE = "CPU cache ways must be a positive integer";

    public static final String MSG_CPU_CORE_ID_NULL = "CPU core ID cannot be NULL";
    public static final String MSG_CPU_CORE_NEGATIVE = "CPU core ID must be in [0, " +
        String.valueOf(CpuCoreId.MAX_CPU_CORE_NB - 1) + "]";
    public static final String MSG_CPU_CORES_NEGATIVE = "Number of CPU cores must be positive";
    public static final String MSG_CPU_LIST_NULL = "Device's set of CPUs cannot be NULL";
    public static final String MSG_CPU_LOAD_NEGATIVE = "CPU load must be in [" + CpuStatistics.MIN_CPU_LOAD +
        ", " + CpuStatistics.MAX_CPU_LOAD + "]";
    public static final String MSG_CPU_FREQUENCY_NEGATIVE = "CPU core frequency must be positive" +
        " and less or equal than " + CpuDevice.MAX_FREQUENCY_MHZ + " MHz";
    public static final String MSG_CPU_SOCKET_NEGATIVE = "CPU socket ID must be in [0, " +
        String.valueOf(CpuCoreId.MAX_CPU_SOCKET_NB - 1) + "]";
    public static final String MSG_CPU_SOCKETS_NEGATIVE = "Number of CPU sockets must be positive";
    public static final String MSG_CPU_VENDOR_NULL = "Unsupported CPU vendor" +
        " Choose one in: " + BasicServerDriver.enumTypesToString(CpuVendor.class);

    public static final String MSG_CONVERSION_TO_BITS = "Invalid conversion to bits";
    public static final String MSG_CONVERSION_TO_BYTES = "Invalid conversion to bytes";

    public static final String MSG_MEM_CAPACITY_NEGATIVE = "Total memory capacity must be positive";
    public static final String MSG_MEM_HIERARCHY_EMPTY = "Memory hierarchy cannot be empty";
    public static final String MSG_MEM_HIERARCHY_NULL = "Memory hierarchy cannot be NULL";
    public static final String MSG_MEM_MANUFACTURER_NULL = "Memory manufacturer cannot be NULL";
    public static final String MSG_MEM_MODULE_NULL = "Memory module cannot be NULL";
    public static final String MSG_MEM_SERIAL_NB_NULL = "Memory serial number cannot be NULL";
    public static final String MSG_MEM_SIZE_NEGATIVE = "Memory size must be positive";
    public static final String MSG_MEM_SPEED_CONF_NEGATIVE = "Configured memory speed must be positive" +
        " and less or equal than total speed";
    public static final String MSG_MEM_SPEED_NEGATIVE = "Memory speed must be positive and less or equal than " +
            MemoryModuleDevice.MAX_SPEED_MTS + " MT/s";
    public static final String MSG_MEM_TYPE_NULL = "Memory type cannot be NULL";
    public static final String MSG_MEM_WIDTH_DATA_NEGATIVE = "Memory data width must be positive";
    public static final String MSG_MEM_WIDTH_TOTAL_NEGATIVE = "Memory total width must be positive";

    public static final String MSG_NIC_FLOW_FILTER_MAC_NULL = "MAC address of NIC Rx filter cannot be NULL";
    public static final String MSG_NIC_FLOW_FILTER_MECH_NULL = "NIC flow Rx filter mechanism cannot be NULL";
    public static final String MSG_NIC_FLOW_FILTER_MPLS_NULL = "MPLS label of NIC Rx filter cannot be NULL";
    public static final String MSG_NIC_FLOW_FILTER_NEGATIVE = "NIC flow Rx filter has invalid CPU core ID";
    public static final String MSG_NIC_FLOW_FILTER_NULL = "NIC flow Rx filter cannot be NULL";
    public static final String MSG_NIC_FLOW_FILTER_RSS_NEGATIVE = "RSS NIC Rx filter cannot be negative";
    public static final String MSG_NIC_FLOW_FILTER_VLAN_NULL = "VLAN ID of NIC Rx filter cannot be NULL";
    public static final String MSG_NIC_FLOW_RULE_ACTION_VAL_NULL = "NIC rule action value cannot be NULL";
    public static final String MSG_NIC_FLOW_RULE_ACTION_TYPE_NULL = "NIC rule action type cannot be NULL";
    public static final String MSG_NIC_FLOW_RULE_CORE_ID_NEGATIVE = "NIC rule's CPU core index must not be negative";
    public static final String MSG_NIC_FLOW_RULE_IFACE_NEGATIVE = "NIC rule's interface number must be positive";
    public static final String MSG_NIC_FLOW_RULE_IFACE_NULL = "NIC rule's interface name cannot be NULL";
    public static final String MSG_NIC_FLOW_RULE_NULL = "NIC flow Rx filter rule cannot be NULL";
    public static final String MSG_NIC_FLOW_RULE_SCOPE_NULL = "NIC rule's scope is NULL or empty";
    public static final String MSG_NIC_FLOW_RULE_TC_ID_NULL = "NIC rule's traffic class ID is NULL or empty";

    public static final String MSG_NIC_LIST_NULL = "Device's set of NICs cannot be NULL";
    public static final String MSG_NIC_NAME_NULL = "NIC name cannot be empty or NULL";
    public static final String MSG_NIC_MAC_NULL = "NIC MAC address cannot be NULL";
    public static final String MSG_NIC_PORT_NUMBER_NEGATIVE = "NIC port number cannot be negative";
    public static final String MSG_NIC_PORT_TYPE_NULL = "NIC port type cannot be NULL";
    public static final String MSG_NIC_RX_FILTER_NULL = "Unsupported NIC Rx filter" +
        " Choose one in: " + BasicServerDriver.enumTypesToString(RxFilter.class);
    public static final String MSG_NIC_RX_FILTERS_NULL = "NIC Rx filters' list is NULL";
    public static final String MSG_NIC_SPEED_NEGATIVE = "NIC speed must be positive and less or equal than " +
        NicDevice.MAX_SPEED + " Mbps";
    public static final String MSG_NIC_TABLE_SIZE_NEGATIVE    = "Invalid NIC table size";
    public static final String MSG_NIC_TABLE_INDEX_NEGATIVE   = "Invalid NIC table index";
    public static final String MSG_NIC_TABLE_COUNTER_NEGATIVE = "Invalid NIC table counter";

    public static final String MSG_STATS_CPU_NULL = "CPU statistics are NULL";
    public static final String MSG_STATS_CPU_CACHE_NULL = "CPU cache statistics are NULL";
    public static final String MSG_STATS_MEMORY_FREE_NEGATIVE = "Free memory must be positive";
    public static final String MSG_STATS_MEMORY_NULL = "Memory statistics are NULL";
    public static final String MSG_STATS_MEMORY_TOTAL_NEGATIVE = "Total memory must be positive";
    public static final String MSG_STATS_MEMORY_USED_NEGATIVE = "Used memory must be positive";
    public static final String MSG_STATS_NIC_NULL = "NIC statistics are NULL";
    public static final String MSG_STATS_TIMING_NULL = "Timing statistics are NULL";
    public static final String MSG_STATS_TIMING_AUTO_SCALE_NEGATIVE = "Auto-scale time must be positive";
    public static final String MSG_STATS_TIMING_DEPLOY_INCONSISTENT = "Deploy time must be equal to the" +
        " summary of parsing and launching times";
    public static final String MSG_STATS_TIMING_LAUNCH_NEGATIVE = "Launching time must be positive";
    public static final String MSG_STATS_TIMING_PARSE_NEGATIVE = "Parsing time must be positive";
    public static final String MSG_STATS_UNIT_NULL = "Statistics unit is NULL";

    public static final String MSG_UI_DATA_CPU_NULL = "No CPU data to visualize";
    public static final String MSG_UI_DATA_LATENCY_NULL = "No latency data to visualize";
    public static final String MSG_UI_DATA_MEMORY_NULL = "No memory data to visualize";
    public static final String MSG_UI_DATA_THROUGHPUT_NULL = "No throughput data to visualize";
    public static final String MSG_UI_SUBMETRIC_NULL = "UI submetric is NULL";

}
