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

import org.onosproject.net.DeviceId;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of stored VPN instance, which contains the configuration
 * such as RD and RT, also the device info and the VPN type.
 */
public class VpnInstance<T extends VpnConfig> {

    /**
     * VPN instance name.
     */
    private String vpnName;

    /**
     * List of devices for the VPN.
     */
    private Map<DeviceId, DeviceInfo> devInfo;

    /**
     * Type of the VPN.
     */
    private VpnType type;

    /**
     * VPN config information.
     */
    private T vpnConfig;

    /**
     * Creates VPN instance with VPN name.
     *
     * @param v VPN name
     */
    public VpnInstance(String v) {
        vpnName = v;
    }

    /**
     * Returns the type of the VPN instance.
     *
     * @return VPN type
     */
    public VpnType type() {
        return type;
    }

    /**
     * Sets the type of the VPN instance.
     *
     * @param type VPN type
     */
    public void type(VpnType type) {
        this.type = type;
    }

    /**
     * Returns the configuration of VPN instance.
     *
     * @return VPN config
     */
    public T vpnConfig() {
        return vpnConfig;
    }

    /**
     * Sets the configuration of VPN instance.
     *
     * @param vpnConfig VPN config
     */
    public void vpnConfig(T vpnConfig) {
        this.vpnConfig = vpnConfig;
    }

    /**
     * Returns the device info map.
     *
     * @return device info map
     */
    public Map<DeviceId, DeviceInfo> devInfo() {
        return devInfo;
    }

    /**
     * Sets the device info map.
     *
     * @param devInfo device info map
     */
    public void devInfo(Map<DeviceId, DeviceInfo> devInfo) {
        this.devInfo = devInfo;
    }

    /**
     * Adds the content to device info map.
     *
     * @param id   device id
     * @param info device info
     */
    public void addDevInfo(DeviceId id, DeviceInfo info) {
        if (devInfo == null) {
            devInfo = new HashMap<>();
        }
        devInfo.put(id, info);
    }

    /**
     * Returns the VPN instance name.
     *
     * @return VPN name
     */
    public String vpnName() {
        return vpnName;
    }
}
