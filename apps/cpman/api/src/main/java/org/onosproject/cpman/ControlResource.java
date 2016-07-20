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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static org.onosproject.cpman.ControlMetricType.CPU_IDLE_TIME;
import static org.onosproject.cpman.ControlMetricType.CPU_LOAD;
import static org.onosproject.cpman.ControlMetricType.DISK_READ_BYTES;
import static org.onosproject.cpman.ControlMetricType.DISK_WRITE_BYTES;
import static org.onosproject.cpman.ControlMetricType.FLOW_MOD_PACKET;
import static org.onosproject.cpman.ControlMetricType.FLOW_REMOVED_PACKET;
import static org.onosproject.cpman.ControlMetricType.INBOUND_PACKET;
import static org.onosproject.cpman.ControlMetricType.MEMORY_FREE;
import static org.onosproject.cpman.ControlMetricType.MEMORY_FREE_RATIO;
import static org.onosproject.cpman.ControlMetricType.MEMORY_USED;
import static org.onosproject.cpman.ControlMetricType.MEMORY_USED_RATIO;
import static org.onosproject.cpman.ControlMetricType.NW_INCOMING_BYTES;
import static org.onosproject.cpman.ControlMetricType.NW_INCOMING_PACKETS;
import static org.onosproject.cpman.ControlMetricType.NW_OUTGOING_BYTES;
import static org.onosproject.cpman.ControlMetricType.NW_OUTGOING_PACKETS;
import static org.onosproject.cpman.ControlMetricType.OUTBOUND_PACKET;
import static org.onosproject.cpman.ControlMetricType.REPLY_PACKET;
import static org.onosproject.cpman.ControlMetricType.REQUEST_PACKET;
import static org.onosproject.cpman.ControlMetricType.SYS_CPU_TIME;
import static org.onosproject.cpman.ControlMetricType.TOTAL_CPU_TIME;
import static org.onosproject.cpman.ControlMetricType.USER_CPU_TIME;

/**
 * A set of resource type used in control plane.
 */
public final class ControlResource {

    private ControlResource() {}

    /**
     * Control resource type.
     */
    public enum Type {
        /* CPU resource */
        CPU,

        /* Memory resource */
        MEMORY,

        /* Disk resource */
        DISK,

        /* Network resource */
        NETWORK,

        /* Control message resource */
        CONTROL_MESSAGE
    }

    /* A collection of CPU related metric types */
    public static final Set<ControlMetricType> CPU_METRICS =
                        ImmutableSet.of(CPU_IDLE_TIME, CPU_LOAD, SYS_CPU_TIME,
                                        USER_CPU_TIME, TOTAL_CPU_TIME);

    /* A collection of memory related metric types */
    public static final Set<ControlMetricType> MEMORY_METRICS =
                        ImmutableSet.of(MEMORY_FREE, MEMORY_FREE_RATIO, MEMORY_USED,
                                        MEMORY_USED_RATIO);

    /* A collection of disk related metric types */
    public static final Set<ControlMetricType> DISK_METRICS =
                        ImmutableSet.of(DISK_READ_BYTES, DISK_WRITE_BYTES);

    /* A collection of network related metric types */
    public static final Set<ControlMetricType> NETWORK_METRICS =
                        ImmutableSet.of(NW_INCOMING_BYTES, NW_OUTGOING_BYTES,
                                        NW_INCOMING_PACKETS, NW_OUTGOING_PACKETS);

    /* A collection of control message related metric types */
    public static final Set<ControlMetricType> CONTROL_MESSAGE_METRICS =
                        ImmutableSet.of(INBOUND_PACKET, OUTBOUND_PACKET, FLOW_MOD_PACKET,
                                        FLOW_REMOVED_PACKET, REQUEST_PACKET, REPLY_PACKET);
}
