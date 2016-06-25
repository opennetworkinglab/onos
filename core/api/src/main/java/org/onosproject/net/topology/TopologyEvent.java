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

import org.onosproject.event.AbstractEvent;
import org.onosproject.event.Event;

import java.util.List;

/**
 * Describes network topology event.
 */
public class TopologyEvent extends AbstractEvent<TopologyEvent.Type, Topology> {

    private final List<Event> reasons;

    /**
     * Type of topology events.
     */
    public enum Type {
        /**
         * Signifies that topology has changed.
         */
        TOPOLOGY_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified topology and the
     * current time.
     *
     * @param type     topology event type
     * @param topology event topology subject
     * @param reasons  list of events that triggered topology change
     */
    public TopologyEvent(Type type, Topology topology, List<Event> reasons) {
        super(type, topology);
        this.reasons = reasons;
    }

    /**
     * Creates an event of a given type and for the specified topology and time.
     *
     * @param type     link event type
     * @param topology event topology subject
     * @param reasons  list of events that triggered topology change
     * @param time     occurrence time
     */
    public TopologyEvent(Type type, Topology topology, List<Event> reasons,
                         long time) {
        super(type, topology, time);
        this.reasons = reasons;
    }


    /**
     * Returns the list of events that triggered the topology change.
     *
     * @return list of events responsible for change in topology; null if
     * initial topology computation
     */
    public List<Event> reasons() {
        return reasons;
    }

}
