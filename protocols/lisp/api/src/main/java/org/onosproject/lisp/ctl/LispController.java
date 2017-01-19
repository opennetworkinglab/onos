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

/**
 * Abstraction of a LISP controller. Serves as a one stop shop for obtaining
 * LISP devices and (un)register listeners on LISP events.
 */
public interface LispController {

    /**
     * Obtains all LISP routers known to this LISP controller.
     *
     * @return Iterable of LISP router elements
     */
    Iterable<LispRouter> getRouters();

    /**
     * Obtains all subscribed LISP routers known to this LISP controllers.
     *
     * @return Iterable of LISP router elements
     */
    Iterable<LispRouter> getSubscribedRouters();

    /**
     * Connects to a specific LISP router.
     * If the connection is established, it creates and adds the device to
     * ONOS core as a LISP router.
     *
     * @param routerId router identifier
     * @return LispRouter LISP router
     */
    LispRouter connectRouter(LispRouterId routerId);

    /**
     * Disconnects a LISP router and notify router removal event.
     *
     * @param routerId router identifier
     * @param remove   true only if want to notify router removal event
     */
    void disconnectRouter(LispRouterId routerId, boolean remove);

    /**
     * Obtains the actual router for the given LispRouterId.
     *
     * @param routerId the router to fetch
     * @return the interface to this router
     */
    LispRouter getRouter(LispRouterId routerId);

    /**
     * Registers a router listener to track router status.
     * (e.g., router add and removal)
     *
     * @param listener the listener to notify
     */
    void addRouterListener(LispRouterListener listener);

    /**
     * Unregisters a router listener.
     *
     * @param listener the listener to unregister
     */
    void removeRouterListener(LispRouterListener listener);

    /**
     * Registers a listener for all LISP message types.
     *
     * @param listener the listener to notify
     */
    void addMessageListener(LispMessageListener listener);

    /**
     * Unregisters a listener.
     *
     * @param listener the listener to unregister
     */
    void removeMessageListener(LispMessageListener listener);
}
