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

/**
 * Known values for IPv6 extension header field that can be supplied to the CLI.
 */
public enum ExtHeader {
    /** No next header. */
    NOEXT((short) (1 << 0)),
    /** Encapsulated Security Payload. */
    ESP((short) (1 << 1)),
    /** Authentication header. */
    AUTH((short) (1 << 2)),
    /** Destination header. */
    DEST((short) (1 << 3)),
    /** Fragment header. */
    FRAG((short) (1 << 4)),
    /** Router header. */
    ROUTE((short) (1 << 5)),
    /** Hop-by-hop header. */
    HOP((short) (1 << 6)),
    /** Unexpected repeats encountered. */
    UNREP((short) (1 << 7)),
    /** Unexpected sequencing encountered. */
    UNSEQ((short) (1 << 8));

    private short value;

    /**
     * Constructs an ExtHeader with the given value.
     *
     * @param value value to use when this ExtHeader is seen
     */
    private ExtHeader(short value) {
        this.value = value;
    }

    /**
     * Gets the value to use for this ExtHeader.
     *
     * @return short value to use for this ExtHeader
     */
    public short value() {
        return this.value;
    }

    /**
     * Parse a string input that could contain an ExtHeader value. The value
     * may appear in the string either as a known exntension header name (one of the
     * values of this enum), or a numeric extension header value.
     *
     * @param input the input string to parse
     * @return the numeric value of the parsed IPv6 extension header
     * @throws IllegalArgumentException if the input string does not contain a
     * value that can be parsed into an IPv6 extension header
     */
    public static short parseFromString(String input) {
        try {
            return valueOf(input).value();
        } catch (IllegalArgumentException e) {
            // The input is not a known IPv6 extension header name, let's see if
            // it's an IPv6 extension header value (short).
            // We parse with Short to handle unsigned values correctly.
            try {
                return Short.parseShort(input);
            } catch (NumberFormatException e1) {
                throw new IllegalArgumentException(
                        "ExtHeader value must be either a string extension header name"
                        + " or an 8-bit extension header value");
            }
        }
    }
}
