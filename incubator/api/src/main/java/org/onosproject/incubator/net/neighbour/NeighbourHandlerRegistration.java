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

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;

/**
 * Information about the registration of a neighbour message handler.
 */
public interface NeighbourHandlerRegistration {

    /**
     * Gets the interface of the registration.
     *
     * @return interface
     */
    Interface intf();

    /**
     * Gets the neighbour message handler.
     *
     * @return message handler
     */
    NeighbourMessageHandler handler();

    /**
     * Gets the ID of the application that registered the handler.
     *
     * @return application ID
     */
    ApplicationId appId();
}
