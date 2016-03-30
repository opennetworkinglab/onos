/*
 * Copyright 2016 Open Networking Laboratory
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

import java.util.List;

/**
 * Representation of an ISIS controller.
 */
public interface IsisController {

    /**
     * Registers a listener for router meta events.
     *
     * @param isisRouterListener isis router listener instance
     */
    void addRouterListener(IsisRouterListener isisRouterListener);

    /**
     * Unregisters a router listener.
     *
     * @param isisRouterListener isis router listener instance
     */
    void removeRouterListener(IsisRouterListener isisRouterListener);

    /**
     * Updates configuration of processes.
     *
     * @param processes process instance to update
     */
    void updateConfig(List<IsisProcess> processes);

    /**
     * Deletes configuration parameters.
     *
     * @param processes list of process instance
     * @param attribute attribute to delete
     */
    void deleteConfig(List<IsisProcess> processes, String attribute);

    /**
     * Gets the all configured processes.
     *
     * @return list of process instances
     */
    List<IsisProcess> allConfiguredProcesses();
}