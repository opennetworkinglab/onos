/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnopenflow.rsc.vpnafconfig;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;
import org.onosproject.evpnrouteservice.VpnRouteTarget;

import java.util.Collection;

/**
 * Service for interacting with the inventory of VPN af config instance.
 */
public interface VpnAfConfigService {
    /**
     * Returns if the route target is existed.
     *
     * @param routeTarget route target
     * @return true or false if one with the given route target is not existed.
     */
    boolean exists(VpnRouteTarget routeTarget);

    /**
     * Returns the VPN af config with the route target.
     *
     * @param routeTarget route target
     * @return VPN af config or null if one with the given route target is not
     * know.
     */
    VpnAfConfig getVpnAfConfig(VpnRouteTarget routeTarget);

    /**
     * Returns the collection of the currently known VPN af configurations.
     *
     * @return collection of VPN af configurations.
     */
    Collection<VpnAfConfig> getVpnAfConfigs();

    /**
     * Creates VPN af configurations by vpnAfConfigs.
     *
     * @param vpnAfConfigs the iterable collection of vpnAfConfigs
     * @return true if all given VPN af configs created successfully
     */
    boolean createVpnAfConfigs(Iterable<VpnAfConfig> vpnAfConfigs);

    /**
     * Updates VPN af configurations by vpnAfConfigs.
     *
     * @param vpnAfConfigs the iterable collection of vpnAfConfigs
     * @return true if all given VPN af configs created successfully.
     */
    boolean updateVpnAfConfigs(Iterable<VpnAfConfig> vpnAfConfigs);

    /**
     * Deletes vpnAfConfigs by route target.
     *
     * @param routeTarget the iterable collection of vpnAFConfigs
     * @return true or false if one with the given route target to delete is
     * successfully
     */
    boolean removeVpnAfConfigs(Iterable<VpnRouteTarget> routeTarget);

    /**
     * process gluon config for vpn af configuration.
     *
     * @param action can be either update or delete
     * @param key    can contain the id and also target information
     * @param value  content of the route targets configuration
     */
    void processGluonConfig(String action, String key, JsonNode value);

    /**
     * Adds the specified listener to Vpn Port manager.
     *
     * @param listener vpn af config listener
     */
    void addListener(VpnAfConfigListener listener);

    /**
     * Removes the specified listener to vpn af config manager.
     *
     * @param listener vpn af config listener
     */
    void removeListener(VpnAfConfigListener listener);
}
