/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.neighbour.NeighbourHandlerRegistration;
import org.onosproject.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.neighbour.NeighbourResolutionService;

import java.util.Collection;
import java.util.Map;

/**
 * Mock Neighbour Resolution Service.
 */
public class MockNeighbourResolutionService implements NeighbourResolutionService {
    @Override
    public void registerNeighbourHandler(ConnectPoint connectPoint,
                                         NeighbourMessageHandler handler, ApplicationId appId) {

    }

    @Override
    public void registerNeighbourHandler(Interface intf,
                                         NeighbourMessageHandler handler, ApplicationId appId) {

    }

    @Override
    public void unregisterNeighbourHandler(ConnectPoint connectPoint,
                                           NeighbourMessageHandler handler, ApplicationId appId) {

    }

    @Override
    public void unregisterNeighbourHandler(Interface intf,
                                           NeighbourMessageHandler handler, ApplicationId appId) {

    }

    @Override
    public void unregisterNeighbourHandlers(ApplicationId appId) {

    }

    @Override
    public Map<ConnectPoint, Collection<NeighbourHandlerRegistration>> getHandlerRegistrations() {
        return null;
    }
}
