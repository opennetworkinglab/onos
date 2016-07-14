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
package org.onlab.packet;

/**
 * Representation of a transport layer port.
 */
public class TpPort {

    private final int port;

    // Transport layer port is unsigned 16 bits integer.
    public static final int MAX_PORT = 0xFFFF;
    public static final int MIN_PORT = 0;

    /**
     * Constructs a new TpPort.
     *
     * @param value the transport layer port
     */
    protected TpPort(int value) {
        this.port = value;
    }

    /**
     * Converts an integer into a TpPort.
     *
     * @param value an integer representing the transport layer port
     * @return a TpPort
     * @throws IllegalArgumentException if the value is invalid
     */
    public static TpPort tpPort(int value) {
        if (value < MIN_PORT || value > MAX_PORT) {
            throw new IllegalArgumentException(
                    "Transport layer port value " + value + "is not in the interval [0, 0xFFFF]");
        }
        return new TpPort(value);
    }

    /**
     * Returns the integer value for this transport port.
     *
     * @return an integer value
     */
    public int toInt() {
        return this.port;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof TpPort) {

            TpPort other = (TpPort) obj;

            if (this.port == other.port) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.port;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.valueOf(this.port);
    }
}
