/*
 * Copyright 2016 Open Networking Laboratory
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
 * The Interface of Openstack Routing.
 */
public interface OpenstackRoutingService {

    /**
     * Stores the Floating IP information created by Openstack.
     *
     * @param openstackFloatingIP Floating IP information
     */
    void createFloatingIP(OpenstackFloatingIP openstackFloatingIP);

    /**
     * Updates flow rules corresponding to the Floating IP information updated by Openstack.
     *
     * @param openstackFloatingIP Floating IP information
     */
    void updateFloatingIP(OpenstackFloatingIP openstackFloatingIP);

    /**
     * Removes flow rules corresponding to Floating IP information removed by Openstack.
     *
     * @param id Deleted Floating IP`s ID
     */
    void deleteFloatingIP(String id);

    /**
     * Stores the router information created by Openstack.
     *
     * @param openstackRouter Floating IP information
     */
    void createRouter(OpenstackRouter openstackRouter);

    /**
     * Updates flow rules corresponding to the router information updated by Openstack.
     *
     * @param openstackRouter Router information
     */
    void updateRouter(OpenstackRouter openstackRouter);

    /**
     * Removes flow rules corresponding to the router information removed by Openstack.
     *
     * @param id Deleted router`s ID
     */
    void deleteRouter(String id);

    /**
     * Updates flow rules corresponding to the router information updated by Openstack.
     *
     * @param openstackRouterInterface Router information
     */
    void updateRouterInterface(OpenstackRouterInterface openstackRouterInterface);

    /**
     * Removes flow rules corresponding to the router information removed by Openstack.
     *
     * @param openstackRouterInterface Router information
     */
    void removeRouterInterface(OpenstackRouterInterface openstackRouterInterface);

    /**
     * Checks floatingIp deassociation when corresponding deleted vm.
     *
     * @param portId Deleted vm
     * @param portInfo
     */
    void checkDisassociatedFloatingIp(String portId, OpenstackPortInfo portInfo);
}
