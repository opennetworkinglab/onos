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

/**
 * Handles floating IP update requests from OpenStack.
 */
public interface OpenstackFloatingIpService {

    enum Action {
        ASSOCIATE,
        DISSASSOCIATE
    }

    /**
     * Handles floating IP create request from OpenStack.
     *
     * @param floatingIp floating IP information
     */
    void createFloatingIp(OpenstackFloatingIP floatingIp);

    /**
     * Handles floating IP update request from OpenStack.
     *
     * @param floatingIp floating IP information
     */
    void updateFloatingIp(OpenstackFloatingIP floatingIp);

    /**
     * Handles floating IP remove request from OpenStack.
     *
     * @param floatingIpId floating ip identifier
     */
    void deleteFloatingIp(String floatingIpId);
}
