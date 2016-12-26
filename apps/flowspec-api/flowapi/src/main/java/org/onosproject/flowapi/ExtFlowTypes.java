/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.flowapi;

/**
 * Representation of BgpFlow container having custom rules.
 */
public interface ExtFlowTypes {

    /**
     * Bgp types.
     */
    public enum ExtType {

        /** Extended flow rule key. */
        EXT_FLOW_RULE_KEY(0),

        /** IPv4 destination address. */
        IPV4_DST_PFX(1),

        /** IPv4 source address. */
        IPV4_SRC_PFX(2),

        /** IP protocol list. */
        IP_PROTO_LIST(3),

        /** Input port list. */
        IN_PORT_LIST(4),

        /** Destination port list. */
        DST_PORT_LIST(5),

        /** Source port list. */
        SRC_PORT_LIST(6),

        /** ICMP type list. */
        ICMP_TYPE_LIST(7),

        /** ICMP code list. */
        ICMP_CODE_LIST(8),

        /** TCP flag list. */
        TCP_FLAG_LIST(9),

        /** Packet length list. */
        PACKET_LENGTH_LIST(10),

        /** DSCP Value component. */
        DSCP_VALUE_LIST(11),

        /** Fragment list. */
        FRAGMENT_LIST(12),

        /** Wide community flags. */
        WIDE_COMM_FLAGS(13),

        /** Wide community hop count. */
        WIDE_COMM_HOP_COUNT(14),

        /** Wide community community attribute. */
        WIDE_COMM_COMMUNITY(15),

        /** Wide community context AS. */
        WIDE_COMM_CONTEXT_AS(16),

        /** Wide community local AS. */
        WIDE_COMM_LOCAL_AS(17),

        /** Wide community target prefixes. */
        WIDE_COMM_TARGET(18),

        /** Wide community extended target prefixes. */
        WIDE_COMM_EXT_TARGET(19),

        /** Wide community parameter. */
        WIDE_COMM_PARAMETER(20),

        /** Traffic filtering actions. */

        TRAFFIC_RATE(0x8006),
        TRAFFIC_ACTION(0x8007),
        TRAFFIC_REDIRECT(0x8008),
        TRAFFIC_MARKING(0x8009);

        private int type;

        /**
         * Creates a new type.
         *
         * @param type type code
         */
        ExtType(int type) {
            this.type = type;
        }

        /**
         * Returns the type object for this type code.
         *
         * @return ExtType object
         */
        public int type() {
            return (type);
        }
    }

    ExtType type();
}
