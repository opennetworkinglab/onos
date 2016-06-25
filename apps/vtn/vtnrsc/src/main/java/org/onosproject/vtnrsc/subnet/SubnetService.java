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
package org.onosproject.vtnrsc.subnet;

import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.SubnetId;


/**
 * Service for interacting with the inventory of subnets.
 */
public interface SubnetService {
    /**
     * Returns the subnet with the specified identifier.
     *
     * @param subnetId subnet identifier
     * @return true or false
     */
    boolean exists(SubnetId subnetId);
    /**
     * Returns a collection of the currently known subnets.
     *
     * @return iterable collection of subnets
     */
    Iterable<Subnet> getSubnets();

    /**
     * Returns the subnet with the specified identifier.
     *
     * @param subnetId subnet identifier
     * @return subnet or null if one with the given identifier is not known
     */
    Subnet getSubnet(SubnetId subnetId);
    /**
     * Creates new subnets.
     *
     * @param subnets the iterable collection of subnets
     * @return true  if the identifier subnet has been created right
     */
    boolean createSubnets(Iterable<Subnet> subnets);

    /**
     * Updates existing subnets.
     *
     * @param subnets the iterable collection of subnets
     * @return true if all subnets were updated successfully
     */
    boolean updateSubnets(Iterable<Subnet> subnets);

    /**
     * Administratively removes the specified subnets from the store.
     *
     * @param subnetIds the iterable collection of  subnets identifier
     * @return true if remove identifier subnets successfully
     */
    boolean removeSubnets(Iterable<SubnetId> subnetIds);


}
