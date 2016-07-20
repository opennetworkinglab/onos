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
package org.onosproject.ospf.controller;

/**
 * Abstraction of an OSPF Router Listener.
 * Allows for providers interested in switch events to be notified.
 */
public interface OspfRouterListener {

    /**
     * Notifies that a router is added.
     *
     * @param ospfRouter OSPF router instance
     */
    void routerAdded(OspfRouter ospfRouter);

    /**
     * Notifies that a router is removed.
     *
     * @param ospfRouter OSPF router instance
     */
    void routerRemoved(OspfRouter ospfRouter);

    /**
     * Notifies that the router has changed in some way.
     *
     * @param ospfRouter OSPF router instance
     */
    void routerChanged(OspfRouter ospfRouter);
}