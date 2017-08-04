/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.device;

import org.onlab.packet.VlanId;

import java.util.List;

/**
 * The description of an interface used for legacy devices.
 */
public interface DeviceInterfaceDescription {
    /**
     * Represents the type of operation of the interface.
     */
    enum Mode {
        /**
         * Normal mode of interface operation.
         */
        NORMAL,
        /**
         * Access mode to a VLAN for interface.
         */
        ACCESS,
        /**
         * Trunk mode for a set of VLANs for interface.
         */
        TRUNK
    }

    /**
     * Returns the name of the interface.
     *
     * @return name of the interface
     */
    String name();

    /**
     *  Returns the operation mode of the interface.
     *
     *  @return operation mode of the interface
     */
    Mode mode();

    /**
     *  Returns the VLAN-IDs configured for the interface. No VLAN-ID should be
     *  returned for NORMAL mode, 1 VLAN-ID for access mode and 1 or more
     *  VLAN-IDs for trunking mode.
     *
     *  @return VLAN-ID(s) configured for the interface.
     */
    List<VlanId> vlans();

    /**
     *  Indicates whether a rate limit has been set on the interface.
     *
     *  @return indication whether interface is rate limited or not
     */
    boolean isRateLimited();

    /**
     *  Returns the rate limit set on the interface bandwidth.
     *
     *  @return the rate limit set on the interface bandwidth
     */
    short rateLimit();
}
