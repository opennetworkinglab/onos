/*
 * Copyright 2015 Open Networking Laboratory
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
 * Represents information for a device to connect to a controller.
 */
public class ControllerInfo {

    public final IpAddress ip;
    public final int tcpPort;

    /**
     * Information for contacting the controller.
     *
     * @param ip the ip address
     * @param tcpPort the tcp port
     */
    public ControllerInfo(IpAddress ip, int tcpPort) {
        this.ip = ip;
        this.tcpPort = tcpPort;
    }
}
