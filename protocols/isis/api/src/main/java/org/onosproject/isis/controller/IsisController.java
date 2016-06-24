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
package org.onosproject.isis.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.isis.controller.topology.IsisLinkListener;
import org.onosproject.isis.controller.topology.IsisRouterListener;

import java.util.List;
import java.util.Set;

/**
 * Representation of an ISIS controller.
 */
public interface IsisController {

    /**
     * Registers a listener for router meta events.
     *
     * @param isisRouterListener ISIS router listener instance
     */
    void addRouterListener(IsisRouterListener isisRouterListener);

    /**
     * Unregisters a router listener.
     *
     * @param isisRouterListener ISIS router listener instance
     */
    void removeRouterListener(IsisRouterListener isisRouterListener);

    /**
     * Updates configuration of processes.
     *
     * @param processesNode json node represents process
     */
    void updateConfig(JsonNode processesNode);

    /**
     * Gets the all configured processes.
     *
     * @return list of process instances
     */
    List<IsisProcess> allConfiguredProcesses();

    /**
     * Registers a listener for ISIS message events.
     *
     * @param listener the listener to notify
     */
    void addLinkListener(IsisLinkListener listener);

    /**
     * Unregisters a link listener.
     *
     * @param listener the listener to unregister
     */
    void removeLinkListener(IsisLinkListener listener);

    /**
     * Gets the list of listeners registered for router events.
     *
     * @return list of listeners
     */
    Set<IsisRouterListener> listener();

    /**
     * Gets the list of listeners registered for link events.
     *
     * @return list of listeners
     */
    Set<IsisLinkListener> linkListener();
}