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
package org.onosproject.isis.controller.topology;

import org.onlab.packet.Ip4Address;

/**
 * Abstraction of an ISIS Router.
 */
public interface IsisRouter {

    /**
     * Returns IP address of the router.
     *
     * @return IP address of the router
     */
    Ip4Address routerIp();

    /**
     * Returns IP address of the interface.
     *
     * @return IP address of the interface
     */
    Ip4Address interfaceId();

    /**
     * Sets IP address of the Router.
     *
     * @param routerIp IP address of the router
     */
    void setRouterIp(Ip4Address routerIp);
}
