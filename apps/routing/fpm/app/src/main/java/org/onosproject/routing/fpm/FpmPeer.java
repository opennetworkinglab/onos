/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.fpm;

import org.onlab.packet.IpAddress;

import java.net.InetSocketAddress;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents an FPM peer.
 */
public class FpmPeer {

    private final IpAddress address;
    private final int port;

    /**
     * Creates a new FPM peer.
     *
     * @param address peer IP address
     * @param port peer TCP port number
     */
    public FpmPeer(IpAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * Returns the peers IP address.
     *
     * @return IP address
     */
    public IpAddress address() {
        return address;
    }

    /**
     * Returns the peer port number.
     *
     * @return port number
     */
    public int port() {
        return port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof FpmPeer)) {
            return false;
        }

        FpmPeer that = (FpmPeer) other;

        return Objects.equals(this.address, that.address) &&
                Objects.equals(this.port, that.port);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("address", address)
                .add("port", port)
                .toString();
    }

    public static FpmPeer fromSocketAddress(InetSocketAddress address) {
        return new FpmPeer(IpAddress.valueOf(address.getAddress()), address.getPort());
    }
}
