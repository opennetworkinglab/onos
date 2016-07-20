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
package org.onosproject.vtnrsc.routerinterface;

import java.util.Collection;

import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SubnetId;

/**
 * Service for interacting with the inventory of Router interface.
 */
public interface RouterInterfaceService {
    /**
     * Returns exists or not of specific subnet identifier.
     *
     * @param subnetId subnet identifier
     * @return true or false
     */
    boolean exists(SubnetId subnetId);

    /**
     * Returns a collection of the currently known Router interface.
     *
     * @return collection of RouterInterface
     */
    Collection<RouterInterface> getRouterInterfaces();

    /**
     * Returns the Router interface with the specified subnet identifier.
     *
     * @param subnetId subnet identifier
     * @return RouterInterface or null if one with the given identifier is not
     *         known
     */
    RouterInterface getRouterInterface(SubnetId subnetId);

    /**
     * Adds the specified RouterInterface.
     *
     * @param routerInterface the interface add to router
     * @return true if add router interface successfully
     */
    boolean addRouterInterface(RouterInterface routerInterface);

    /**
     * Removes the specified RouterInterface.
     *
     * @param routerInterface the interface remove from router
     * @return true if remove router interface successfully
     */
    boolean removeRouterInterface(RouterInterface routerInterface);

    /**
     * Adds the specified listener to Router Interface manager.
     *
     * @param listener Router Interface listener
     */
    void addListener(RouterInterfaceListener listener);

    /**
     * Removes the specified listener to RouterInterface manager.
     *
     * @param listener Router Interface listener
     */
    void removeListener(RouterInterfaceListener listener);
}
