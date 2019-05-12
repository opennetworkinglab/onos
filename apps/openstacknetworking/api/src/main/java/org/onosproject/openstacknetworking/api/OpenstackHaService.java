/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.api;

import org.onlab.packet.IpAddress;

/**
 * Handles openstack mastership.
 */
public interface OpenstackHaService {

    /**
     * Indicates the active-standby status.
     *
     * @return true if the the node runs in active mode, false otherwise
     */
    boolean isActive();

    /**
     * Obtains the IP address of the active node.
     *
     * @return IP address of the active node
     */
    IpAddress getActiveIp();

    /**
     * Configures the IP address of the active node.
     *
     * @param ip  IP address of the active node
     */
    void setActiveIp(IpAddress ip);

    /**
     * Configures the active status.
     *
     * @param flag true if the node runs in active mode, false otherwise
     */
    void setActive(boolean flag);
}
