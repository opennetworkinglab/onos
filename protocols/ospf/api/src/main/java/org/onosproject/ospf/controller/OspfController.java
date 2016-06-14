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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

/**
 * Abstraction of an OSPF controller.
 * Serves as a one stop shop for obtaining OSPF devices and (un)register listeners on OSPF events.
 */
public interface OspfController {

    /**
     * Registers a listener for router meta events.
     *
     * @param listener the listener to notify
     */
    void addRouterListener(OspfRouterListener listener);

    /**
     * Unregisters a router listener.
     *
     * @param listener the listener to unregister
     */
    void removeRouterListener(OspfRouterListener listener);

    /**
     * Registers a listener for OSPF message events.
     *
     * @param listener the listener to notify
     */
    void addLinkListener(OspfLinkListener listener);

    /**
     * Unregisters a link listener.
     *
     * @param listener the listener to unregister
     */
    void removeLinkListener(OspfLinkListener listener);

    /**
     * Updates configuration of processes.
     *
     * @param processesNode process info to update
     */
    void updateConfig(JsonNode processesNode);

    /**
     * Deletes configuration parameters.
     *
     * @param processes list of process instance
     * @param attribute attribute to delete
     */
    void deleteConfig(List<OspfProcess> processes, String attribute);

    /**
     * Gets the list of listeners registered for router events.
     *
     * @return list of listeners
     */
    Set<OspfRouterListener> listener();

    /**
     * Gets the list of listeners registered for link events.
     *
     * @return list of listeners
     */
    public Set<OspfLinkListener> linkListener();

    /**
     * Gets the configured process.
     *
     * @return list of process instances
     */
    public List<OspfProcess> getAllConfiguredProcesses();
}