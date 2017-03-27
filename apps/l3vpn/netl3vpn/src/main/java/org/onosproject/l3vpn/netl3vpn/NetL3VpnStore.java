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

import java.util.Map;

/**
 * Abstraction of an entity providing pool of available VPN instances
 * its associated devices and interface information.
 */
public interface NetL3VpnStore {

    /**
     * Returns the freed ids that can be re-used for RD and RT generation.
     *
     * @return collection of freed ids
     */
    Iterable<Long> getFreedIdList();

    /**
     * Returns the VPN instance map available in the store.
     *
     * @return VPN instance map
     */
    Map<String, VpnInstance> getVpnInstances();

    /**
     * Returns the BGP info map available in the store.
     *
     * @return BGP info map
     */
    Map<BgpInfo, DeviceId> getBgpInfo();

    /**
     * Returns the interface information map available in the store.
     *
     * @return interface info map
     */
    Map<AccessInfo, InterfaceInfo> getInterfaceInfo();

    /**
     * Adds freed id to the freed list in the store.
     *
     * @param id id
     */
    void addIdToFreeList(Long id);

    /**
     * Adds the VPN name and the VPN instance, if the map does'nt have the
     * value with it.
     *
     * @param name     VPN name
     * @param instance VPN instance
     */
    void addVpnInsIfAbsent(String name, VpnInstance instance);

    /**
     * Adds the VPN name and the VPN instance to the map.
     *
     * @param name     VPN name
     * @param instance VPN instance
     */
    void addVpnIns(String name, VpnInstance instance);

    /**
     * Adds the access info and the interface info to the map in store.
     *
     * @param accessInfo access info
     * @param intInfo    interface info
     */
    void addInterfaceInfo(AccessInfo accessInfo, InterfaceInfo intInfo);

    /**
     * Adds the BGP info and the device id to the map in store.
     *
     * @param bgpInfo BGP info
     * @param devId   device id
     */
    void addBgpInfo(BgpInfo bgpInfo, DeviceId devId);

    /**
     * Removes the interface info with the key access info from the store.
     *
     * @param accessInfo access info
     * @return true if removed; false otherwise
     */
    boolean removeInterfaceInfo(AccessInfo accessInfo);

    /**
     * Removes the VPN instance from the store with the key VPN name from the
     * store.
     *
     * @param vpnName VPN name
     * @return true if removed; false otherwise
     */
    boolean removeVpnInstance(String vpnName);

    /**
     * Removes the mentioned id from the freed list.
     *
     * @param id id
     * @return true if removed; false otherwise
     */
    boolean removeIdFromFreeList(Long id);

    /**
     * Removes the device id from the store with the key BGP info from the
     * store.
     *
     * @param bgpInfo BGP info
     * @return true if removed; false otherwise
     */
    boolean removeBgpInfo(BgpInfo bgpInfo);
}
