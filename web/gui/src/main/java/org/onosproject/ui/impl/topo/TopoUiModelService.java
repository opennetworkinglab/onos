/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**t
 * Defines the API for the Topology UI Model.
 */
public interface TopoUiModelService {

    /**
     * Registers the specified listener for Topology UI Model events.
     *
     * @param listener the listener
     */
    void addListener(TopoUiListener listener);

    /**
     * Unregister the specified listener.
     *
     * @param listener the listener
     */
    void removeListener(TopoUiListener listener);


    /**
     * Returns events describing the current state of the model.
     * <p>
     * These will be in the form of "addInstance", "addDevice", "addLink",
     * and "addHost" events, as appropriate.
     *
     * @return initial state messages
     */
    List<ObjectNode> getInitialState();

    /**
     * Starts the summary monitoring process.
     * <p>
     * Sends a "showSummary" message now, and schedules a task to send
     * updates whenever the data changes.
     */
    void startSummaryMonitoring();

    /**
     * Cancels the task that sends summary updates.
     */
    void stopSummaryMonitoring();

    /**
     * Returns base data about the topology.
     *
     * @return summary data
     */
    SummaryData getSummaryData();
}
