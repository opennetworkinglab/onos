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
package org.onosproject.net.resource;

import java.util.Objects;

/**
 * Representation of bandwidth resource in bps.
 */
public final class Bandwidth extends LinkResource {

    private final double bandwidth;

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param bandwidth bandwidth value to be assigned
     */
    private Bandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    // Constructor for serialization
    private Bandwidth() {
        this.bandwidth = 0;
    }

    /**
     * Creates a new instance with given bandwidth in bps.
     *
     * @param bandwidth bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    @Deprecated
    public static Bandwidth valueOf(double bandwidth) {
        return bps(bandwidth);
    }

    /**
     * Creates a new instance with given bandwidth in bps.
     *
     * @param bandwidth bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth bps(double bandwidth) {
        return new Bandwidth(bandwidth);
    }

    /**
     * Creates a new instance with given bandwidth in Kbps.
     *
     * @param bandwidth bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth kbps(double bandwidth) {
        return new Bandwidth(bandwidth * 1_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Mbps.
     *
     * @param bandwidth bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth mbps(double bandwidth) {
        return new Bandwidth(bandwidth * 1_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Gbps.
     *
     * @param bandwidth bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth gbps(double bandwidth) {
        return new Bandwidth(bandwidth * 1_000_000_000L);
    }

    /**
     * Returns bandwidth as a double value.
     *
     * @return bandwidth as a double value
     */
    public double toDouble() {
        return bandwidth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bandwidth) {
            Bandwidth that = (Bandwidth) obj;
            return Objects.equals(this.bandwidth, that.bandwidth);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.bandwidth);
    }

    @Override
    public String toString() {
        return String.valueOf(this.bandwidth);
    }
}
