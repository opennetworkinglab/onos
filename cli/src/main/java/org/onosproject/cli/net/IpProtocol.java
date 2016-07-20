/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;

/**
 * Known protocol values for IP protocol field that can be supplied to the CLI.
 */
public enum IpProtocol {
    /** ICMP. **/
    ICMP(IPv4.PROTOCOL_ICMP),
    /** TCP. **/
    TCP(IPv4.PROTOCOL_TCP),
    /** UDP. **/
    UDP(IPv4.PROTOCOL_UDP),
    /** ICMP6. **/
    ICMP6(IPv6.PROTOCOL_ICMP6);

    private short value;

    /**
     * Constructs an IpProtocol with the given value.
     *
     * @param value value to use when this IpProtocol is seen
     */
    private IpProtocol(short value) {
        this.value = value;
    }

    /**
     * Gets the value to use for this IpProtocol.
     *
     * @return short value to use for this IpProtocol
     */
    public short value() {
        return this.value;
    }

    /**
     * Parse a string input that could contain an IpProtocol value. The value
     * may appear in the string either as a known protocol name (one of the
     * values of this enum), or a numeric protocol value.
     *
     * @param input the input string to parse
     * @return the numeric value of the parsed IP protocol
     * @throws IllegalArgumentException if the input string does not contain a
     * value that can be parsed into an IP protocol
     */
    public static short parseFromString(String input) {
        try {
            return valueOf(input).value();
        } catch (IllegalArgumentException e) {
            // The input is not a known IP protocol name, let's see if it's an IP
            // protocol value (byte). We parse with Short to handle unsigned values
            // correctly.
            try {
                return Short.parseShort(input);
            } catch (NumberFormatException e1) {
                throw new IllegalArgumentException(
                        "IpProtocol value must be either a string protocol name"
                        + " or an 8-bit protocol value");
            }
        }
    }
}
