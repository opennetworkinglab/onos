/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.cluster;

import org.onlab.packet.IpAddress;

/**
 * Represents a controller instance as a member in a cluster.
 */
public interface Node {

    /**
     * Returns the instance identifier.
     *
     * @return instance identifier
     */
    NodeId id();

    /**
     * Returns the IP address of the controller instance.
     *
     * @return IP address
     */
    default IpAddress ip() {
        return ip(false);
    }

    /**
     * Returns the IP address of the controller instance.
     *
     * @param resolve whether to resolve the hostname
     * @return IP address
     */
    IpAddress ip(boolean resolve);

    /**
     * Returns the host name of the controller instance.
     *
     * @return the host name of the controller instance
     */
    String host();

    /**
     * Returns the TCP port on which the node listens for connections.
     *
     * @return TCP port
     */
    int tcpPort();

}
