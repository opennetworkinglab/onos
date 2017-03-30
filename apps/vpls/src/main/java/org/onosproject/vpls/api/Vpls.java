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

package org.onosproject.vpls.api;

import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.EncapsulationType;

import java.util.Collection;

public interface Vpls {

    /**
     * Creates a new VPLS.
     *
     * @param vplsName the name of the VPLS
     * @param encapsulationType the encapsulation type
     * @return a VPLS instance if the operation is successful; null otherwise
     */
    VplsData createVpls(String vplsName, EncapsulationType encapsulationType);

    /**
     * Removes a VPLS.
     *
     * @param vplsData the VPLS to be removed
     * @return the VPLS removed if the operation is successful; null otherwise
     */
    VplsData removeVpls(VplsData vplsData);

    /**
     * Adds network interfaces to a VPLS.
     *
     * @param vplsData the VPLS to which the interfaces have to be added to
     * @param interfaces the interfaces to add
     */
    void addInterfaces(VplsData vplsData, Collection<Interface> interfaces);

    /**
     * Adds a network interface to a VPLS.
     *
     * @param vplsData the VPLS to which the interface has to be added to
     * @param iface the interface to add
     */
    void addInterface(VplsData vplsData, Interface iface);

    /**
     * Sets an encapsulation type for a VPLS.
     *
     * @param vplsData the VPLS for which the encapsulation has to be set
     * @param encapsulationType the encapsulation type
     */
    void setEncapsulationType(VplsData vplsData, EncapsulationType encapsulationType);

    /**
     * Retrieves a VPLS.
     *
     * @param vplsName the name of the VPLS
     * @return the VPLS instance if the VPLS exists; null otherwise
     */
    VplsData getVpls(String vplsName);

    /**
     * Gets all VPLSs.
     *
     * @return a collection of VPLSs
     */
    Collection<VplsData> getAllVpls();

    /**
     * Removes the interfaces specified from a VPLS.
     *
     * @param vplsData the VPLS from which the interfaces are to be removed
     * @param interfaces the interfaces to remove
     * @return the interfaces removed
     */
    Collection<Interface> removeInterfaces(VplsData vplsData, Collection<Interface> interfaces);

    /**
     * Removes the interface specified from a VPLS.
     *
     * @param vplsData the VPLS from which the interface is to be removed
     * @param iface the interface to remove
     * @return the interface removed
     */
    Interface removeInterface(VplsData vplsData, Interface iface);

    /**
     * Removes all VPLSs and cleans up the VPLS configuration.
     */
    void removeAllVpls();
}
