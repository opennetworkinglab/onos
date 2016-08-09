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

package org.onosproject.incubator.net.neighbour;

import com.google.common.annotations.Beta;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;

/**
 * Provides a means of registering logic for handling neighbour messages.
 */
@Beta
public interface NeighbourResolutionService {

    /**
     * Registers a neighbour message handler for all neighbour messages
     * incoming on the given connect point.
     *
     * @param connectPoint connect point
     * @param handler neighbour message handler
     */
    void registerNeighbourHandler(ConnectPoint connectPoint, NeighbourMessageHandler handler);

    /**
     * Registers a neighbour message handler for all neighbour messages incoming
     * on the given interface. Neighbour packets must match the fields of the
     * interface in order to be handled by this message handler.
     *
     * @param intf interface
     * @param handler neighbour message handler
     */
    void registerNeighbourHandler(Interface intf, NeighbourMessageHandler handler);

}
