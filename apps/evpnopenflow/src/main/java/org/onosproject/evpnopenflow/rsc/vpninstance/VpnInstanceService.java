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

package org.onosproject.evpnopenflow.rsc.vpninstance;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.evpnopenflow.rsc.VpnInstance;
import org.onosproject.evpnopenflow.rsc.VpnInstanceId;
import org.onosproject.evpnrouteservice.VpnRouteTarget;

import java.util.Collection;
import java.util.Set;

/**
 * Service for interacting with the inventory of VPN instance.
 */
public interface VpnInstanceService {
    /**
     * Returns if the vpnInstance is existed.
     *
     * @param vpnInstanceId vpnInstance identifier
     * @return true or false if one with the given identifier is not existed.
     */
    boolean exists(VpnInstanceId vpnInstanceId);

    /**
     * Returns the vpnInstance with the identifier.
     *
     * @param vpnInstanceId vpnInstance ID
     * @return VpnInstance or null if one with the given ID is not know.
     */
    VpnInstance getInstance(VpnInstanceId vpnInstanceId);

    /**
     * Returns the collection of the currently known vpnInstance.
     *
     * @return collection of VpnInstance.
     */
    Collection<VpnInstance> getInstances();

    /**
     * Creates vpnInstances by vpnInstances.
     *
     * @param vpnInstances the iterable collection of vpnInstances
     * @return true if all given identifiers created successfully.
     */
    boolean createInstances(Iterable<VpnInstance> vpnInstances);

    /**
     * Updates vpnInstances by vpnInstances.
     *
     * @param vpnInstances the iterable  collection of vpnInstances
     * @return true if all given identifiers updated successfully.
     */
    boolean updateInstances(Iterable<VpnInstance> vpnInstances);

    /**
     * Deletes vpnInstanceIds by vpnInstanceIds.
     *
     * @param vpnInstanceIds the iterable collection of vpnInstance identifiers
     * @return true or false if one with the given identifier to delete is
     * successfully.
     */
    boolean removeInstances(Iterable<VpnInstanceId> vpnInstanceIds);

    /**
     * process gluon config for vpn instance information.
     *
     * @param action can be either update or delete
     * @param key    can contain the id and also target information
     * @param value  content of the vpn instance configuration
     */
    void processGluonConfig(String action, String key, JsonNode value);

    /**
     * process Etcd response for vpn instance information.
     *
     * @param routeTargetType    route target type
     * @param exportRouteTargets export route targets
     * @param importRouteTargets import route targets
     * @param vpnRouteTarget     vpn route target
     */
    void updateImpExpRouteTargets(String routeTargetType,
                                  Set<VpnRouteTarget> exportRouteTargets,
                                  Set<VpnRouteTarget> importRouteTargets,
                                  VpnRouteTarget vpnRouteTarget);
}
