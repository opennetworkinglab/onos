/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Known values for ICMPv6 type field that can be supplied to the CLI.
 */
public enum Icmp6Type {
    /** Destination Unreachable. */
    DEST_UNREACH(ICMP6.DEST_UNREACH),
    /** Packet Too Big. */
    PKT_TOO_BIG(ICMP6.PKT_TOO_BIG),
    /** Time Exceeded. */
    TIME_EXCEED(ICMP6.TIME_EXCEED),
    /** Parameter Problem. */
    PARAM_ERR(ICMP6.PARAM_ERR),
    /** Echo Request. */
    ECHO_REQUEST(ICMP6.ECHO_REQUEST),
    /** Echo Reply. */
    ECHO_REPLY(ICMP6.ECHO_REPLY),
    /** Multicast Listener Query. */
    MCAST_QUERY(ICMP6.MCAST_QUERY),
    /** Multicast Listener Report. */
    MCAST_REPORT(ICMP6.MCAST_REPORT),
    /** Multicast Listener Done. */
    MCAST_DONE(ICMP6.MCAST_DONE),
    /** Router Solicitation. */
    ROUTER_SOLICITATION(ICMP6.ROUTER_SOLICITATION),
    /** Router Advertisement. */
    ROUTER_ADVERTISEMENT(ICMP6.ROUTER_ADVERTISEMENT),
    /** Neighbor Solicitation. */
    NEIGHBOR_SOLICITATION(ICMP6.NEIGHBOR_SOLICITATION),
    /** Neighbor Advertisement. */
    NEIGHBOR_ADVERTISEMENT(ICMP6.NEIGHBOR_ADVERTISEMENT),
    /** Redirect Message. */
    REDIRECT(ICMP6.REDIRECT);


    private byte value;

    /**
     * Constructs an Icmp6Type with the given value.
     *
     * @param value value to use when this Icmp6Type is seen
     */
    private Icmp6Type(byte value) {
        this.value = value;
    }

    /**
     * Gets the value to use for this Icmp6Type.
     *
     * @return short value to use for this Icmp6Type
     */
    public byte value() {
        return this.value;
    }

    /**
     * Parse a string input that could contain an Icmp6Type value. The value
     * may appear in the string either as a known type name (one of the
     * values of this enum), or a numeric type value.
     *
     * @param input the input string to parse
     * @return the numeric value of the parsed ICMPv6 type
     * @throws IllegalArgumentException if the input string does not contain a
     * value that can be parsed into an ICMPv6 type
     */
    public static byte parseFromString(String input) {
        try {
            return valueOf(input).value();
        } catch (IllegalArgumentException e) {
            // The input is not a known ICMPv6 type name, let's see if it's an ICMP6
            // type value (byte). We parse with Byte to handle unsigned values
            // correctly.
            try {
                return Byte.parseByte(input);
            } catch (NumberFormatException e1) {
                throw new IllegalArgumentException(
                        "Icmp6Type value must be either a string type name"
                        + " or an 8-bit type value");
            }
        }
    }
}
