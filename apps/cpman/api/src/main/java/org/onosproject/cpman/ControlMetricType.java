/*
 * Copyright 2016-present Open Networking Laboratory
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

/**
 * A set of metric type used in control plane.
 */
public enum ControlMetricType {

    /* Mapped to PACKET-IN message of OpenFlow. */
    INBOUND_PACKET,

    /* Mapped to PACKET-OUT message of OpenFlow. */
    OUTBOUND_PACKET,

    /* Mapped to FLOW-MOD message of OpenFlow. */
    FLOW_MOD_PACKET,

    /* Mapped to FLOW-REMOVED message of OpenFlow. */
    FLOW_REMOVED_PACKET,

    /* Mapped to STATS-REQUEST message of OpenFlow. */
    REQUEST_PACKET,

    /* Mapped to STATS-REPLY message of OpenFlow. */
    REPLY_PACKET,

    /* Number of CPU cores. */
    NUM_OF_CORES,

    /* Number of CPUs. */
    NUM_OF_CPUS,

    /* CPU Speed. */
    CPU_SPEED,

    /* CPU Load. */
    CPU_LOAD,

    /* Total Amount of CPU Up Time. */
    TOTAL_CPU_TIME,

    /* System CPU Up Time. */
    SYS_CPU_TIME,

    /* User CPU Up Time. */
    USER_CPU_TIME,

    /* CPU Idle Time. */
    CPU_IDLE_TIME,

    /* Ratio of Used Memory Amount. */
    MEMORY_USED_RATIO,

    /* Ratio of Free Memory Amount. */
    MEMORY_FREE_RATIO,

    /* Used Memory Amount. */
    MEMORY_USED,

    /* Free Memory Amount. */
    MEMORY_FREE,

    /* Total Amount of Memory. */
    MEMORY_TOTAL,

    /* Disk Read Bytes. */
    DISK_READ_BYTES,

    /* Disk Write Bytes. */
    DISK_WRITE_BYTES,

    /* Network Incoming Bytes. */
    NW_INCOMING_BYTES,

    /* Network Outgoing Bytes. */
    NW_OUTGOING_BYTES,

    /* Network Incoming Packets. */
    NW_INCOMING_PACKETS,

    /* Network Outgoing Packets. */
    NW_OUTGOING_PACKETS
}
