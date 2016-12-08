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
package org.onosproject.lisp.ctl;

import org.onosproject.lisp.msg.protocols.LispMessage;

/**
 * Responsible for keeping track of the current set of
 * routers connected to the system.
 */
public interface LispRouterAgent {

    /**
     * Adds a router that has just connected to the system.
     * We try to add the router into connectedRouter pool when the router is
     * initially sending Map-Register message to the controller.
     *
     * @param routerId the routerId to add
     * @param router   the actual router object
     * @return true if added, false otherwise
     */
    boolean addConnectedRouter(LispRouterId routerId, LispRouter router);

    /**
     * Clears all state in controller router maps for a router that has
     * disconnected from the local controller. Also release control for that
     * router from the global repository. Notify router listener.
     *
     * @param routerId the routerId to rmove
     */
    void removeConnectedRouter(LispRouterId routerId);

    /**
     * Processes a message coming from a router. Notifies message listeners on
     * all incoming message event.
     *
     * @param routerId the routerId of a router where the message comes from
     * @param message  the message to process
     */
    void processUpstreamMessage(LispRouterId routerId, LispMessage message);

    /**
     * Processes a message going to a router. Notifies message listeners on
     * all outgoing message event.
     *
     * @param routerId the routerId of a router where the message goes to
     * @param message  the message to process
     */
    void processDownstreamMessage(LispRouterId routerId, LispMessage message);
}
