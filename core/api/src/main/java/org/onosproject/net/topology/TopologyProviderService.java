/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.topology;

import org.onosproject.event.Event;
import org.onosproject.net.provider.ProviderService;

import java.util.List;

/**
 * Means for injecting topology information into the core.
 */
public interface TopologyProviderService extends ProviderService<TopologyProvider> {

    /**
     * Signals the core that some aspect of the topology has changed.
     *
     * @param graphDescription information about the network graph
     * @param reasons          events that triggered topology change
     */
    void topologyChanged(GraphDescription graphDescription,
                         List<Event> reasons);

}
