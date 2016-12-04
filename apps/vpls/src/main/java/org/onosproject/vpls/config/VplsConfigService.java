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
import org.onosproject.net.EncapsulationType;

import java.util.Map;
import java.util.Set;

/**
 * Provides information about the VPLS configuration.
 */
public interface VplsConfigService {
    Class<VplsAppConfig> CONFIG_CLASS = VplsAppConfig.class;

    /**
     * Adds a VPLS to the configuration.
     *
     * @param vplsName the name of the VPLS
     * @param ifaces the interfaces associated with the VPLS
     * @param encap the encapsulation type
     */
    void addVpls(String vplsName, Set<String> ifaces, String encap);

    /**
     * Removes a VPLS from the configuration.
     *
     * @param vplsName the name of the VPLS to be removed
     */
    void removeVpls(String vplsName);

    /**
     * Adds a network interface to a VPLS.
     *
     * @param vplsName the name of the VPLS
     * @param iface the network interface to be added to the VPLS
     */
    void addIface(String vplsName, String iface);

    /**
     * Sets an encapsulation parameter for a VPLS.
     *
     * @param vplsName the name of the VPLS
     * @param encap the encapsulation used (i.e. MPLS or VLAN) or
     */
    void setEncap(String vplsName, String encap);

    /**
     * Returns the encapsulation type in use for a given VPLS.
     *
     * @param vplsName the name of the VPLS
     * @return the encapsulation type in use, if any
     */
    EncapsulationType encap(String vplsName);

    /**
     * Removes a network interface from a VPLS.
     *
     * @param iface the network interface to be removed from the VPLS
     */
    void removeIface(String iface);

    /**
     * Cleans up the VPLS configuration. Removes all VPLSs.
     */
    void cleanVplsConfig();

    /**
     * Retrieves the VPLS names modified from CLI.
     *
     * @return the VPLS names modified from CLI
     */
    Set<String> vplsAffectedByApi();
    // TODO Remove this function after the intent framework race condition has been fixed

    /**
     * Retrieves the interfaces from the VPLS configuration.
     *
     * @return a set of interfaces contained in the VPLS configuration
     */
    Set<Interface> allIfaces();

    /**
     * Retrieves the interfaces from the VPLS configuration.
     *
     * @return a set of interfaces belonging to the VPLS
     */
    Set<Interface> ifaces();

    /**
     * Retrieves the interfaces belonging to the VPLS.
     *
     * @param vplsName the name of the VPLS
     * @return a set of interfaces belonging to the VPLS
     */
    Set<Interface> ifaces(String vplsName);

    /**
     * Retrieves all VPLS names.
     *
     * @return a set of VPLS names
     */
    Set<String> vplsNames();

    /**
     * Retrieves all VPLS names from the old config.
     *
     * @return a set of VPLS names
     */
    Set<String> vplsNamesOld();
    // TODO Removes this function after intent framework fix race condition

    /**
     * Returns the VPLS names and associated interfaces from the configuration.
     *
     * @return a map of VPLS names and associated interfaces
     */
    SetMultimap<String, Interface> ifacesByVplsName();

    /**
     * Returns the list of interfaces grouped by VPLS name, given a VLAN Id and
     * a connect point.
     *
     * @param vlan the VLAN Id
     * @param connectPoint the connect point
     * @return a map of VPLS names and associated interfaces; null otherwise
     */
    SetMultimap<String, Interface> ifacesByVplsName(VlanId vlan,
                                                    ConnectPoint connectPoint);

    /**
     * Returns the VPLS names and associated encapsulation type.
     *
     * @return a map of VPLS names and associated encapsulation type
     */
    Map<String, EncapsulationType> encapByVplsName();
}
