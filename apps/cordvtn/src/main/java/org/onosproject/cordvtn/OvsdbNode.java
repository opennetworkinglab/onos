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

import java.util.Comparator;

/**
 * Representation of a node with ovsdb server.
 */
public interface OvsdbNode {

    Comparator<OvsdbNode> OVSDB_NODE_COMPARATOR = new Comparator<OvsdbNode>() {
        @Override
        public int compare(OvsdbNode ovsdb1, OvsdbNode ovsdb2) {
            return ovsdb1.host().compareTo(ovsdb2.host());
        }
    };

    /**
     * Returns the IP address of the ovsdb server.
     *
     * @return ip address
     */
    IpAddress ip();

    /**
     * Returns the port number of the ovsdb server.
     *
     * @return port number
     */
    TpPort port();

    /**
     * Returns the host information of the ovsdb server.
     * It could be hostname or ip address.
     *
     * @return host
     */
    String host();

    /**
     * Returns the device id of the ovsdb server.
     *
     * @return device id
     */
    DeviceId deviceId();

    /**
     * Returns the device id of the integration bridge associated with the node.
     *
     * @return device id
     */
    DeviceId intBrId();
}
