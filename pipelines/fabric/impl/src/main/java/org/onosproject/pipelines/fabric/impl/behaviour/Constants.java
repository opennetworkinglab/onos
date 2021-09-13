/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Constant values.
 */
public final class Constants {

    // Used with is_infra_port metadata
    public static final byte[] ONE = new byte[]{1};
    public static final byte[] ZERO = new byte[]{0};

    public static final byte FALSE = (byte) 0x00;
    public static final byte TRUE = (byte) 0x01;

    public static final long PORT_TYPE_MASK = 0x3;
    public static final byte PORT_TYPE_EDGE = 0x1;
    public static final byte PORT_TYPE_INFRA = 0x2;
    public static final byte PORT_TYPE_INTERNAL = 0x3;

    // Forwarding types from P4 program (not exposed in P4Info).
    public static final byte FWD_MPLS = 1;
    public static final byte FWD_IPV4_ROUTING = 2;
    public static final byte FWD_IPV6_ROUTING = 4;

    public static final short ETH_TYPE_EXACT_MASK = (short) 0xFFFF;

    public static final int DEFAULT_VLAN = 4094;
    public static final int DEFAULT_PW_TRANSPORT_VLAN = 4090;

    // Default Slice and Traffic Class IDs
    public static final int DEFAULT_SLICE_ID = 0;
    public static final int DEFAULT_TC = 0;
    public static final byte DEFAULT_QFI = (byte) 0x00;

    //////////////////////////////////////////////////////////////////////////////
    // 64 .... 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1 0 //
    //  X      X  X  X  X  X  X  X  X  X  X  X  X  X  X  X  X X X X X 1 1 1 1 1 //
    //////////////////////////////////////////////////////////////////////////////
    // Metadata instruction is used as 8 byte sequence to carry up to 64 metadata

    // FIXME We are assuming SR as the only app programming this meta.
    // SDFAB-530 to get rid of this limitation

    /**
     * SR is setting this metadata when a double tagged filtering objective is removed
     * and no other hosts is sharing the same input port. Thus, termination mac entries
     * can be removed together with the vlan table entries.
     *
     * See org.onosproject.segmentrouting.RoutingRulePopulator#buildDoubleTaggedFilteringObj()
     * See org.onosproject.segmentrouting.RoutingRulePopulator#processDoubleTaggedFilter()
     */
    public static final long CLEANUP_DOUBLE_TAGGED_HOST_ENTRIES = 1;

    /**
     * SR is setting this metadata when an interface config update has been performed
     * and thus termination mac entries should not be removed.
     *
     * See org.onosproject.segmentrouting.RoutingRulePopulator#processSinglePortFiltersInternal
     */
    public static final long INTERFACE_CONFIG_UPDATE = 1L << 1;

    /**
     * SR is setting this metadata to signal the driver when the config is for the pair port,
     * i.e. ports connecting two leaves.
     *
     *  See org.onosproject.segmentrouting.RoutingRulePopulator#portType
     */
    public static final long PAIR_PORT = 1L << 2;

    /**
     * SR is setting this metadata to signal the driver when the config is for an edge port,
     * i.e. ports facing an host.
     *
     * See org.onosproject.segmentrouting.policy.impl.PolicyManager#trafficMatchFwdObjective
     * See org.onosproject.segmentrouting.RoutingRulePopulator#portType
     */
    public static final long EDGE_PORT = 1L << 3;

    /**
     * SR is setting this metadata to signal the driver when the config is for an infra port,
     * i.e. ports connecting a leaf with a spine.
     */
    public static final long INFRA_PORT = 1L << 4;

    public static final long METADATA_MASK = 0x1FL;

    public static final Map<Long, Byte> METADATA_TO_PORT_TYPE = ImmutableMap.<Long, Byte>builder()
            .put(PAIR_PORT, PORT_TYPE_INFRA)
            .put(EDGE_PORT, PORT_TYPE_EDGE)
            .put(INFRA_PORT, PORT_TYPE_INFRA)
            .build();

    // hide default constructor
    private Constants() {
    }
}