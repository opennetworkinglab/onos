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

import org.onosproject.net.host.HostService;

/**
 * Handler for an incoming neighbour message.
 *
 * <p>An application may implement this interface in order to provide their own
 * logic for handling particular neighbour messages.</p>
 */
public interface NeighbourMessageHandler {

    /**
     * Handles a neighbour message.
     *
     * @param context neighbour message context
     * @param hostService host service
     */
    void handleMessage(NeighbourMessageContext context, HostService hostService);
}
