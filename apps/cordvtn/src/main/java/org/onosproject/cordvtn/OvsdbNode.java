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
package org.onosproject.cordvtn;

import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;

/**
 * Representation of a node with ovsdb server.
 */
public interface OvsdbNode {
    /**
     * State of the ovsdb node.
     */
    enum State {
        INIT, READY, CONNECTED, DISCONNECTED
    }

    /**
     * Returns the IP address of ovsdb server.
     *
     * @return ip address
     */
    IpAddress ip();

    /**
     * Returns the port number of ovsdb server.
     *
     * @return port number
     */
    TpPort port();

    /**
     * Returns the hostname of the node.
     *
     * @return hostname
     */
    String hostname();

    /**
     * Returns the state of the node.
     *
     * @return state of the node
     */
    State state();

    /**
     * Returns the device ID of the node.
     *
     * @return device id
     */
    DeviceId deviceId();

    /**
     * Returns the device ID of the bridge associated with this node.
     *
     * @return device id
     */
    DeviceId bridgeId();
}
