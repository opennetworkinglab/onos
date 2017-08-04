/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onlab.packet.Ethernet;

/**
 * Allowed values for Ethernet types.  Used by the CLI completer for
 * connectivity based intent L2 parameters.
 */
public enum EthType {
    /** ARP. */
    ARP(Ethernet.TYPE_ARP),
    /** RARP. */
    RARP(Ethernet.TYPE_RARP),
    /** IPV4. */
    IPV4(Ethernet.TYPE_IPV4),
    /** IPV6. */
    IPV6(Ethernet.TYPE_IPV6),
    /** LLDP. */
    LLDP(Ethernet.TYPE_LLDP),
    /** BSN. */
    BSN(Ethernet.TYPE_BSN);

    private short value;

    /**
     * Constructs an EthType with the given value.
     *
     * @param value value to use when this EthType is seen
     */
    private EthType(short value) {
        this.value = value;
    }

    /**
     * Gets the value to use for this EthType.
     *
     * @return short value to use for this EthType
     */
    public short value() {
        return this.value;
    }

    /**
     * Parse a string input that could contain an EthType value. The value
     * may appear in the string either as a known protocol name (one of the
     * values of this enum), or a numeric protocol value.
     *
     * @param input the input string to parse
     * @return the numeric value of the parsed Ethernet type
     * @throws IllegalArgumentException if the input string does not contain a
     * value that can be parsed into an Ethernet type
     */
    public static short parseFromString(String input) {
        try {
            return valueOf(input).value();
        } catch (IllegalArgumentException e) {
            // The input is not a known Ethernet type name, let's see if it's an
            // Ethernet type value (short). We parse with Integer to handle
            // unsigned values correctly.
            try {
                return (short) Integer.parseInt(input);
            } catch (NumberFormatException e1) {
                throw new IllegalArgumentException(
                        "EthType value must be either a string protocol name"
                        + " or a 16-bit protocol value");
            }
        }
    }
}
