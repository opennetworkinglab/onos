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

package org.onosproject.isis.controller.topology;

/**
 * Representation of an ISIS agent.
 * It is responsible for keeping track of the current set of routers
 * connected to the system.
 */
public interface IsisAgent {
    /**
     * Adds a router that has just connected to the system.
     *
     * @param isisRouter the router id to add
     * @return true if added, false otherwise
     */
    boolean addConnectedRouter(IsisRouter isisRouter);

    /**
     * Removes the router which got disconnected from the system.
     *
     * @param isisRouter the router id to remove
     */
    void removeConnectedRouter(IsisRouter isisRouter);

    /**
     * Notifies that got a packet of link from network and need do processing.
     *
     * @param isisLink  link instance
     */
    void addLink(IsisLink isisLink);

    /**
     * Notifies that got a packet of link from network and need do processing.
     *
     * @param isisLink  link instance
     */
    void deleteLink(IsisLink isisLink);
}