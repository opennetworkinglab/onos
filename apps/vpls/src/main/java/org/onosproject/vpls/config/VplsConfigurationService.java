/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vpls.config;

import com.google.common.collect.SetMultimap;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

/**
 * Provides information about the VPLS configuration.
 */
public interface VplsConfigurationService {
    Class<VplsConfig> CONFIG_CLASS = VplsConfig.class;

    /**
     * Adds a VPLS to the configuration.
     *
     * @param name the name of the VPLS
     * @param ifaces the interfaces associated with the VPLS
     */
    void addVpls(String name, Set<String> ifaces);

    /**
     * Removes a VPLS from the configuration.
     *
     * @param name the name of the VPLS to be removed
     */
    void removeVpls(String name);

    /**
     * Adds a network interface to a VPLS.
     *
     * @param name the name of the VPLS
     * @param iface the network interface to be added to the VPLS
     */
    void addInterfaceToVpls(String name, String iface);

    /**
     * Removes a network interface from a VPLS.
     *
     * @param iface the network interface to be removed from the VPLS
     */
    void removeInterfaceFromVpls(String iface);

    /**
     * Cleans up the VPLS configuration. Removes all VPLSs.
     */
    void cleanVpls();

    /**
     * Retrieves the VPLS names modified from CLI.
     *
     * @return a set of VPLS names modified from CLI
     */
    Set<String> getVplsAffectedByApi();
    // TODO Removes this function after intent framework fix race condition

    /**
     * Retrieves the interfaces from the VPLS configuration.
     *
     * @return a set of interfaces contained in the VPLS configuration
     */
    Set<Interface> getAllInterfaces();

    /**
     * Retrieves the interfaces belonging to the VPLS.
     *
     * @param name the name of the VPLS
     * @return a set of interfaces belonging to the VPLS
     */
    Set<Interface> getVplsInterfaces(String name);

    /**
     * Retrieves all VPLS names.
     *
     * @return a set of VPLS names
     */
    Set<String> getAllVpls();

    /**
     * Retrieves all VPLS names from the old config.
     *
     * @return a set of VPLS names
     */
    Set<String> getOldVpls();
    // TODO Removes this function after intent framework fix race condition

    /**
     * Retrieves the VPLS names and associated interfaces from the configuration.
     *
     * @return a map VPLS names and associated interfaces
     */
    SetMultimap<String, Interface> getVplsNetworks();

    /**
     * Retrieves a VPLS network given a VLAN Id and a connect point.
     *
     * @param vlan the VLAN Id
     * @param connectPoint the connect point
     * @return a map VPLS names and associated interfaces; null otherwise
     */
    SetMultimap<String, Interface> getVplsNetwork(VlanId vlan,
                                                 ConnectPoint connectPoint);
}
