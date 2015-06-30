/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.primitives.UnsignedLongs;

/**
 * Representation of a port number.
 */
public final class PortNumber {

    public static final PortNumber P0 = portNumber(0);

    // TODO: revisit the max and the logical port value assignments

    private static final long MAX_NUMBER = (2L * Integer.MAX_VALUE) + 1;


    static final long IN_PORT_NUMBER = -8L;
    static final long TABLE_NUMBER = -7L;
    static final long NORMAL_NUMBER = -6L;
    static final long FLOOD_NUMBER = -5L;
    static final long ALL_NUMBER = -4L;
    static final long LOCAL_NUMBER = -2L;
    static final long CONTROLLER_NUMBER = -3L;

    public static final PortNumber IN_PORT = new PortNumber(IN_PORT_NUMBER);
    public static final PortNumber TABLE = new PortNumber(TABLE_NUMBER);
    public static final PortNumber NORMAL = new PortNumber(NORMAL_NUMBER);
    public static final PortNumber FLOOD = new PortNumber(FLOOD_NUMBER);
    public static final PortNumber ALL = new PortNumber(ALL_NUMBER);
    public static final PortNumber LOCAL = new PortNumber(LOCAL_NUMBER);
    public static final PortNumber CONTROLLER = new PortNumber(CONTROLLER_NUMBER);

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
     * @param string port number as string value
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
        if (number == CONTROLLER_NUMBER) {
            return "CONTROLLER";
        } else if (number == LOCAL_NUMBER) {
            return "LOCAL";
        } else if (number == ALL_NUMBER) {
            return "ALL";
        } else if (number == FLOOD_NUMBER) {
            return "FLOOD";
        } else if (number == NORMAL_NUMBER) {
            return "NORMAL";
        } else if (number == TABLE_NUMBER) {
            return "TABLE";
        } else if (number == IN_PORT_NUMBER) {
            return "IN_PORT";
        }
        return "UNKNOWN";
    }

    @Override
    public String toString() {
        if (!isLogical()) {
            return name;
        } else {
            return decodeLogicalPort();
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
}
