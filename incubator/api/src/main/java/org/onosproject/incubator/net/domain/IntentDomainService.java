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
 */
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;
import org.onlab.graph.Graph;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Service for that maintains a graph of intent domains and a registry of intent
 * domain providers.
 */
@Beta
public interface IntentDomainService {

    /**
     * Returns the intent domain for the given id.
     *
     * @param id id to look up
     * @return the intent domain; null if none found
     */
    IntentDomain getDomain(IntentDomainId id);

    /**
     * Returns a set of all intent domains.
     *
     * @return set of intent domains
     */
    Set<IntentDomain> getDomains();

    /**
     * Returns any network domains associated with the given device id.
     *
     * @param deviceId device id to look up
     * @return set of intent domain
     */
    Set<IntentDomain> getDomains(DeviceId deviceId);

    /**
     * Returns the graph of intent domains and connection devices.
     *
     * @return graph of network domains
     */
    Graph<DomainVertex, DomainEdge> getDomainGraph();

    /**
     * Adds the specified listener for intent domain events.
     *
     * @param listener listener to be added
     */
    void addListener(IntentDomainListener listener);

    /**
     * Removes the specified listener for intent domain events.
     *
     * @param listener listener to be removed
     */
    void removeListener(IntentDomainListener listener);
}





