/*
 * Copyright 2014 Open Networking Laboratory
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

import java.util.Objects;

import com.google.common.primitives.UnsignedLongs;

/**
 * Representation of a port number.
 */
public final class PortNumber {

    public static final PortNumber P0 = portNumber(0);

    // TODO: revisit the max and the logical port value assignments

    private static final long MAX_NUMBER = (2L * Integer.MAX_VALUE) + 1;


    public static final PortNumber IN_PORT = new PortNumber(-8);
    public static final PortNumber TABLE = new PortNumber(-7);
    public static final PortNumber NORMAL = new PortNumber(-6);
    public static final PortNumber FLOOD = new PortNumber(-5);
    public static final PortNumber ALL = new PortNumber(-4);
    public static final PortNumber LOCAL = new PortNumber(-2);

    private final long number;

    // Public creation is prohibited
    private PortNumber(long number) {
        this.number = number;
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
     * Indicates whether or not this port number is a reserved logical one or
     * whether it corresponds to a normal physical port of a device or NIC.
     *
     * @return true if logical port number
     */
    public boolean isLogical() {
        return number < 0 || number > MAX_NUMBER;
    }

    /**
     * Returns the backing long value.
     *
     * @return port number as long
     */
    public long toLong() {
        return number;
    }

    @Override
    public String toString() {
        return UnsignedLongs.toString(number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
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
