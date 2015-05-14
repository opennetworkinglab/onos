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

/**
 * Provides basic summary data for the topology.
 */
// TODO: review -- move to core-api module?
public interface SummaryData {

    /**
     * Returns the number of devices.
     *
     * @return number of devices
     */
    int deviceCount();

    /**
     * Returns the number of links.
     *
     * @return number of links
     */
    int linkCount();

    /**
     * Returns the number of hosts.
     *
     * @return number of hosts
     */
    int hostCount();

    /**
     * Returns the number of clusters (topology SCCs).
     *
     * @return number of clusters
     */
    int clusterCount();

    /**
     * Returns the number of intents in the system.
     *
     * @return number of intents
     */
    long intentCount();

    /**
     * Returns the number of flow rules in the system.
     *
     * @return number of flow rules
     */
    int flowRuleCount();

}
