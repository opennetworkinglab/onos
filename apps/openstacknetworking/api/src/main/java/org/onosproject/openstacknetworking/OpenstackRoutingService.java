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

import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstackinterface.OpenstackRouterInterface;

/**
 * Handles router update requests from OpenStack.
 */
public interface OpenstackRoutingService {

    /**
     * Handles the router create request from OpenStack.
     *
     * @param osRouter router information
     */
    void createRouter(OpenstackRouter osRouter);

    /**
     * Handles the router update request from OpenStack.
     * Update router is called when the name, administrative state, or the external
     * gateway setting is updated. The external gateway update is the only case
     * that openstack routing service cares.
     *
     * @param osRouter router information
     */
    void updateRouter(OpenstackRouter osRouter);

    /**
     * Handles the router remove request from OpenStack.
     *
     * @param osRouterId identifier of the router
     */
    void removeRouter(String osRouterId);

    /**
     * Handles router interface add request from OpenStack.
     *
     * @param osInterface router interface information
     */
    void addRouterInterface(OpenstackRouterInterface osInterface);

    /**
     * Handles router interface remove request from OpenStack.
     *
     * @param osInterface router interface information
     */
    void removeRouterInterface(OpenstackRouterInterface osInterface);
}
