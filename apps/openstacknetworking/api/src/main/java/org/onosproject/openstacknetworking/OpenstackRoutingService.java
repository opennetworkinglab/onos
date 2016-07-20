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
package org.onosproject.openstacknetworking;

import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstackinterface.OpenstackRouterInterface;

/**
 * Supports L3 management REST API for openstack.
 */
public interface OpenstackRoutingService {

    /**
     * Stores the floating IP information created by openstack.
     *
     * @param openstackFloatingIp Floating IP information
     */
    void createFloatingIP(OpenstackFloatingIP openstackFloatingIp);

    /**
     * Updates flow rules corresponding to the floating IP information updated by openstack.
     *
     * @param openstackFloatingIp Floating IP information
     */
    void updateFloatingIP(OpenstackFloatingIP openstackFloatingIp);

    /**
     * Removes flow rules corresponding to floating IP information removed by openstack.
     *
     * @param id Deleted Floating IP`s ID
     */
    void deleteFloatingIP(String id);

    /**
     * Stores the router information created by openstack.
     *
     * @param openstackRouter Router information
     */
    void createRouter(OpenstackRouter openstackRouter);

    /**
     * Updates flow rules corresponding to the router information updated by openstack.
     *
     * @param openstackRouter Router information
     */
    void updateRouter(OpenstackRouter openstackRouter);

    /**
     * Removes flow rules corresponding to the router information removed by openstack.
     *
     * @param id Deleted router`s ID
     */
    void deleteRouter(String id);

    /**
     * Updates flow rules corresponding to the router information updated by openstack.
     *
     * @param openstackRouterInterface Router interface information
     */
    void updateRouterInterface(OpenstackRouterInterface openstackRouterInterface);

    /**
     * Removes flow rules corresponding to the router information removed by openstack.
     *
     * @param openstackRouterInterface Router interface information
     */
    void removeRouterInterface(OpenstackRouterInterface openstackRouterInterface);

    /**
     * Checks floatingIp disassociation when corresponding deleted vm.
     *
     * @param portId Deleted vm
     * @param portInfo stored information about deleted vm
     */
    void checkDisassociatedFloatingIp(String portId, OpenstackPortInfo portInfo);

    /**
     * Returns network id for routerInterface.
     *
     * @param portId routerInterface`s port id
     */
    String networkIdForRouterInterface(String portId);
}
