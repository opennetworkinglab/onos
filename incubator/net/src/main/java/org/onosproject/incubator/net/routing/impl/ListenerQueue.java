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

package org.onosproject.incubator.net.routing.impl;

import org.onosproject.incubator.net.routing.RouteEvent;

/**
 * Queues updates for a route listener to ensure they are received in the
 * correct order.
 */
interface ListenerQueue {

    /**
     * Posts an event to the listener.
     *
     * @param event event
     */
    void post(RouteEvent event);

    /**
     * Initiates event delivery to the listener.
     */
    void start();

    /**
     * Halts event delivery to the listener.
     */
    void stop();
}
