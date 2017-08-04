/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.cli.net;

import org.onlab.packet.ICMP6;

/**
 * Known values for ICMPv6 code field that can be supplied to the CLI.
 */
public enum Icmp6Code {
    // Code for DEST_UNREACH
    /** No route to destination. */
    NO_ROUTE(ICMP6.NO_ROUTE),
    /** Communication with destination administratively prohibited. */
    COMM_PROHIBIT(ICMP6.COMM_PROHIBIT),
    /** Beyond scope of source address. */
    BEYOND_SCOPE(ICMP6.BEYOND_SCOPE),
    /** Address unreachable. */
    ADDR_UNREACH(ICMP6.ADDR_UNREACH),
    /** Port unreachable. */
    PORT_UNREACH(ICMP6.PORT_UNREACH),
    /** Source address failed ingress/egress policy. */
    FAIL_POLICY(ICMP6.FAIL_POLICY),
    /** Reject route to destination. */
    REJECT_ROUTE(ICMP6.REJECT_ROUTE),
    /** Error in Source Routing Header. */
    SRC_ROUTING_HEADER_ERR(ICMP6.SRC_ROUTING_HEADER_ERR),

    // Code for TIME_EXCEED
    /** Hop limit exceeded in transit. */
    HOP_LIMIT_EXCEED(ICMP6.HOP_LIMIT_EXCEED),
    /** Fragment reassembly time exceeded. */
    DEFRAG_TIME_EXCEED(ICMP6.DEFRAG_TIME_EXCEED),

    // Code for PARAM_ERR
    /** Erroneous header field encountered. */
    HDR_FIELD_ERR(ICMP6.HDR_FIELD_ERR),
    /** Unrecognized Next Header type encountered. */
    NEXT_HEADER_ERR(ICMP6.NEXT_HEADER_ERR),
    /** Unrecognized IPv6 option encountered. */
    IPV6_OPT_ERR(ICMP6.IPV6_OPT_ERR);

    private byte value;

    /**
     * Constructs an Icmp6Code with the given value.
     *
     * @param value value to use when this Icmp6Code is seen
     */
    private Icmp6Code(byte value) {
        this.value = value;
    }

    /**
     * Gets the value to use for this Icmp6Code.
     *
     * @return short value to use for this Icmp6Code
     */
    public byte value() {
        return this.value;
    }

    /**
     * Parse a string input that could contain an Icmp6Code value. The value
     * may appear in the string either as a known code name (one of the
     * values of this enum), or a numeric code value.
     *
     * @param input the input string to parse
     * @return the numeric value of the parsed ICMPv6 code
     * @throws IllegalArgumentException if the input string does not contain a
     * value that can be parsed into an ICMPv6 code
     */
    public static byte parseFromString(String input) {
        try {
            return valueOf(input).value();
        } catch (IllegalArgumentException e) {
            // The input is not a known ICMPv6 code name, let's see if it's an ICMP6
            // code value (byte). We parse with Byte to handle unsigned values
            // correctly.
            try {
                return Byte.parseByte(input);
            } catch (NumberFormatException e1) {
                throw new IllegalArgumentException(
                        "Icmp6Code value must be either a string code name"
                        + " or an 8-bit code value");
            }
        }
    }
}
