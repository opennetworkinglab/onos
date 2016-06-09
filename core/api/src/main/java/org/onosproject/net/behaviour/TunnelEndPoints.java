/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import org.onlab.packet.IpAddress;

/**
 * Static utility methods pertaining to {@link TunnelEndPoint} instances.
 */
public final class TunnelEndPoints {

    private TunnelEndPoints() {
    }

    private static TunnelEndPoint<String> flowEndpoint = new TunnelEndPoint<>("flow");

    /**
     * Returns a tunnel endpoint with supplied IP address.
     *
     * @param ipAddress ip address
     * @return tunnel endpoint instance
     */
    public static TunnelEndPoint<IpAddress> ipTunnelEndpoint(IpAddress ipAddress) {
        return new TunnelEndPoint<>(ipAddress);
    }

    /**
     * Returns a tunnel endpoint with FLOW keyword.
     *
     * @return tunnel endpoint instance
     */
    public static TunnelEndPoint<String> flowTunnelEndpoint() {
        return flowEndpoint;
    }
}
