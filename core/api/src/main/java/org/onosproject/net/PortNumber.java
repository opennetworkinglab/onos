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
package org.onosproject.net;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedLongs;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a port number.
 */
public final class PortNumber {

    public static final PortNumber P0 = portNumber(0);

    // TODO: revisit the max and the logical port value assignments

    static final long MAX_NUMBER = (2L * Integer.MAX_VALUE) + 1;

    static final long IN_PORT_NUMBER = -8L;
    static final long TABLE_NUMBER = -7L;
    static final long NORMAL_NUMBER = -6L;
    static final long FLOOD_NUMBER = -5L;
    static final long ALL_NUMBER = -4L;
    static final long CONTROLLER_NUMBER = -3L;
    static final long LOCAL_NUMBER = -2L;
    static final long ANY_NUMBER = -1L;

    /**
     * Logical PortNumbers.
     */
    public enum Logical {
        IN_PORT(IN_PORT_NUMBER),
        TABLE(TABLE_NUMBER),
        NORMAL(NORMAL_NUMBER),
        FLOOD(FLOOD_NUMBER),
        ALL(ALL_NUMBER),
        LOCAL(LOCAL_NUMBER),
        CONTROLLER(CONTROLLER_NUMBER),
        ANY(ANY_NUMBER);

        private final long number;
        private final PortNumber instance;

        public long number() {
            return number;
        }

        /**
         * PortNumber instance for the logical port.
         *
         * @return {@link PortNumber}
         */
        public PortNumber instance() {
            return instance;
        }

        Logical(long number) {
            this.number = number;
            this.instance = new PortNumber(number);
        }
    }

    public static final PortNumber IN_PORT = new PortNumber(IN_PORT_NUMBER);
    public static final PortNumber TABLE = new PortNumber(TABLE_NUMBER);
    public static final PortNumber NORMAL = new PortNumber(NORMAL_NUMBER);
    public static final PortNumber FLOOD = new PortNumber(FLOOD_NUMBER);
    public static final PortNumber ALL = new PortNumber(ALL_NUMBER);
    public static final PortNumber LOCAL = new PortNumber(LOCAL_NUMBER);
    public static final PortNumber CONTROLLER = new PortNumber(CONTROLLER_NUMBER);
    public static final PortNumber ANY = new PortNumber(ANY_NUMBER);

    // lazily populated Logical port number to PortNumber
    static final Supplier<Map<Long, Logical>> LOGICAL = Suppliers.memoize(() -> {
            Builder<Long, Logical> builder = ImmutableMap.builder();
            for (Logical lp : Logical.values()) {
                builder.put(lp.number(), lp);
            }
            return builder.build();
        });

    private final long number;
    private final String name;
    private final boolean hasName;

    // Public creation is prohibited
    private PortNumber(long number) {
        this.number = number;
        this.name = UnsignedLongs.toString(number);
        this.hasName = false;
    }

    private PortNumber(long number, String name) {
        this.number = number;
        this.name = name;
        this.hasName = true;
    }

    /**
     * Returns the port number representing the specified long value.
     *
     * @param number port number as long value
     * @return port number
     */
    public static PortNumber portNumber(long number) {
        return new PortNumber(number);
    }

    /**
     * Returns the port number representing the specified string value.
     *
     * @param string port number as decimal, hexadecimal, or octal number string
     * @return port number
     */
    public static PortNumber portNumber(String string) {
        return new PortNumber(UnsignedLongs.decode(string));
    }

    /**
     * Returns the port number representing the specified long value and name.
     *
     * @param number port number as long value
     * @param name port name as string value
     * @return port number
     */
    public static PortNumber portNumber(long number, String name) {
        return new PortNumber(number, name);
    }

    /**
     * Returns PortNumber instance from String representation.
     *
     * @param s String representation equivalent to {@link PortNumber#toString()}
     * @return {@link PortNumber} instance
     * @throws IllegalArgumentException if given String was malformed
     */
    public static PortNumber fromString(String s) {
        checkNotNull(s);
        checkArgument(!s.isEmpty(), "cannot be empty");

        if (isAsciiDecimal(s.charAt(0))) {
            // unsigned decimal string
            return portNumber(s);
        } else if (s.startsWith("[")) {
            // named PortNumber
            Matcher matcher = NAMED.matcher(s);
            checkArgument(matcher.matches(), "Invalid named PortNumber %s", s);

            String name = matcher.group("name");
            String num = matcher.group("num");
            return portNumber(UnsignedLongs.parseUnsignedLong(num), name);
        }

        // Logical
        if (s.startsWith("UNKNOWN(") && s.endsWith(")")) {
            return portNumber(s.substring("UNKNOWN(".length(), s.length() - 1));
        } else {
            return Logical.valueOf(s).instance;
        }
    }

    /**
     * Indicates whether or not this port number is a reserved logical one or
     * whether it corresponds to a normal physical port of a device or NIC.
     *
     * @return true if logical port number
     */
    public boolean isLogical() {
        if (hasName) {
            return false;
        } else {
            return (number < 0 || number > MAX_NUMBER);
        }
    }

    /**
     * Returns the backing long value.
     *
     * @return port number as long
     */
    public long toLong() {
        return number;
    }

    /**
     * Returns the backing string value.
     *
     * @return port name as string value
     */
    public String name() {
        return name;
    }

    /**
     * Indicates whether this port number was created with a port name,
     * or only with a number.
     *
     * @return true if port was created with name
     */
    public boolean hasName() {
        return hasName;
    }

    private String decodeLogicalPort() {
        Logical logical = LOGICAL.get().get(number);
        if (logical != null) {
            // enum name
            return logical.toString();
        }
        return String.format("UNKNOWN(%s)", UnsignedLongs.toString(number));
    }


    /**
     * Regular expression to match String representation of named PortNumber.
     *
     * Format: "[name](num:unsigned decimal string)"
     */
    private static final Pattern NAMED = Pattern.compile("^\\[(?<name>.*)\\]\\((?<num>\\d+)\\)$");

    private static boolean isAsciiDecimal(char c) {
        return '0' <= c  && c <= '9';
    }

    @Override
    public String toString() {
        if (isLogical()) {
            return decodeLogicalPort();
        } else if (hasName()) {
            // named port
            return String.format("[%s](%d)", name, number);
        } else {
            // unsigned decimal string
            return name;
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(number);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PortNumber) {
            final PortNumber other = (PortNumber) obj;
            return this.number == other.number;
        }
        return false;
    }

    /**
     * Indicates whether some other PortNumber object is equal to this one
     * including it's name.
     *
     * @param that other {@link PortNumber} instance to compare
     * @return true if equal, false otherwise
     */
    public boolean exactlyEquals(PortNumber that) {
        if (this == that) {
            return true;
        }

        return this.equals(that) &&
               this.hasName == that.hasName &&
               Objects.equal(this.name, that.name);
    }
}
