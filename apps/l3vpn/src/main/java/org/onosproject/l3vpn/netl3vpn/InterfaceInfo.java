/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.l3vpn.netl3vpn;

/**
 * Representation of interface information, which has the interface name and
 * its binding VPN name and the device info to which it belongs to.
 */
public class InterfaceInfo {

    /**
     * Device info value.
     */
    private DeviceInfo devInfo;

    /**
     * Interface name.
     */
    private String intName;

    /**
     * VPN instance name.
     */
    private String vpnName;

    /**
     * Constructs interface info.
     *
     * @param d device info
     * @param i interface name
     * @param v VPN name
     */
    public InterfaceInfo(DeviceInfo d, String i, String v) {
        devInfo = d;
        intName = i;
        vpnName = v;
    }

    /**
     * Returns device info of the interface.
     *
     * @return device info
     */
    public DeviceInfo devInfo() {
        return devInfo;
    }

    /**
     * Returns the interface name.
     *
     * @return interface name
     */
    public String intName() {
        return intName;
    }

    /**
     * Returns the VPN name.
     *
     * @return VPN name
     */
    public String vpnName() {
        return vpnName;
    }
}
