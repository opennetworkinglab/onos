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
package org.onosproject.net.pi.runtime;

import org.onosproject.net.pi.model.PiActionProfileId;

/**
 * Constants for Pi* unit tests.
 */
public final class PiConstantsTest {

    static final String DOT = ".";
    static final String MOD_NW_DST = "mod_nw_dst";
    static final String DEC_TTL = "dec_ttl";
    static final String MOD_VLAN_VID = "mod_vlan_vid";
    public static final String DROP = "drop";

    static final String IPV4_HEADER_NAME = "ipv4_t";
    static final String ETH_HEADER_NAME = "ethernet_t";
    static final String VLAN_HEADER_NAME = "vlan_tag_t";

    public static final String ETH_TYPE = "etherType";
    public static final String DST_ADDR = "dstAddr";
    static final String SRC_ADDR = "srcAddr";
    static final String VID = "vid";
    public static final String PORT = "port";

    static final String EGRESS_PORT = "egress_port";
    static final String INGRESS_PORT = "ingress_port";

    static final PiActionProfileId ACTION_PROF_ID =
            PiActionProfileId.of("Test action profile");

    private PiConstantsTest() {
        // Hides constructor.
    }
}
