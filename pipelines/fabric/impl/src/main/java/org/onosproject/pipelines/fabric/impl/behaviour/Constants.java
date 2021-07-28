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


    // hide default constructor
    private Constants() {
    }
}