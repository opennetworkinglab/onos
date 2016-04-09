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

package org.onosproject.netconf;

/**
 * Interface representing a NETCONF device.
 */
public interface NetconfDevice {


    /**
     * Returns whether a device is a NETCONF device with a capabilities list
     * and is accessible.
     *
     * @return true if device is accessible, false otherwise
     */
    boolean isActive();

    /**
     * Returns a NETCONF session context for this device.
     *
     * @return netconf session
     */
    NetconfSession getSession();

    /**
     * Ensures that all sessions are closed.
     * A device cannot be used after disconnect is called.
     */
    void disconnect();

    /**
     * return all the info associated with this device.
     * @return NetconfDeviceInfo
     */
    NetconfDeviceInfo getDeviceInfo();
}