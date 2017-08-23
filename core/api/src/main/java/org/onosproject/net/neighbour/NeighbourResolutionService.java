/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.neighbour;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.ConnectPoint;

import java.util.Collection;
import java.util.Map;

/**
 * Provides a means of registering logic for handling neighbour messages.
 */
public interface NeighbourResolutionService {

    /**
     * Registers a neighbour message handler for all neighbour messages
     * incoming on the given connect point.
     *
     * @param connectPoint connect point
     * @param handler neighbour message handler
     * @param appId application ID
     */
    void registerNeighbourHandler(ConnectPoint connectPoint, NeighbourMessageHandler handler,
                                  ApplicationId appId);

    /**
     * Registers a neighbour message handler for all neighbour messages incoming
     * on the given interface. Neighbour packets must match the fields of the
     * interface in order to be handled by this message handler.
     *
     * @param intf interface
     * @param handler neighbour message handler
     * @param appId application ID
     */
    void registerNeighbourHandler(Interface intf, NeighbourMessageHandler handler,
                                  ApplicationId appId);

    /**
     * Unregisters a neighbour message handler that was assigned to a connect
     * point.
     *
     * @param connectPoint connect point
     * @param handler neighbour message handler
     * @param appId application ID
     */
    void unregisterNeighbourHandler(ConnectPoint connectPoint, NeighbourMessageHandler handler,
                                    ApplicationId appId);

    /**
     * Unregisters a neighbour message handler that was assigned to an interface.
     *
     * @param intf interface
     * @param handler neighbour message handler
     * @param appId application ID
     */
    void unregisterNeighbourHandler(Interface intf, NeighbourMessageHandler handler,
                                    ApplicationId appId);

    /**
     * Unregisters all neighbour handlers that were registered by the given
     * application.
     *
     * @param appId application ID
     */
    void unregisterNeighbourHandlers(ApplicationId appId);

    /**
     * Gets the neighbour message handlers that have been registered with the
     * service.
     *
     * @return neighbour message handlers indexed by connect point
     */
    Map<ConnectPoint, Collection<NeighbourHandlerRegistration>> getHandlerRegistrations();
}
