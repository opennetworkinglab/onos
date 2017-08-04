/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.ofagent.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes OFAgent event.
 */
public class OFAgentEvent extends AbstractEvent<OFAgentEvent.Type, OFAgent> {

    private final OFController controller;

    public enum Type {

        /**
         * Signifies that a new OFAgent is created.
         */
        OFAGENT_CREATED,

        /**
         * Signifies that the OFAgent is removed.
         */
        OFAGENT_REMOVED,

        /**
         * Signifies that the new external controller is added.
         */
        OFAGENT_CONTROLLER_ADDED,

        /**
         * Signifies that the external controller is removed.
         */
        OFAGENT_CONTROLLER_REMOVED,

        /**
         * Signifies that the OFAgent is started.
         */
        OFAGENT_STARTED,

        /**
         * Signifies that the OFAgent is stopped.
         */
        OFAGENT_STOPPED,
    }

    /**
     * Creates an event of a given type for the specified ofagent and the current time.
     *
     * @param type    ofagent event type
     * @param ofAgent ofagent instance
     */
    public OFAgentEvent(OFAgentEvent.Type type, OFAgent ofAgent) {
        super(type, ofAgent);
        this.controller = null;
    }

    /**
     * Creates an event of a given type for the specified ofagent and the updated controller.
     *
     * @param type       ofagent event type
     * @param ofAgent    ofagent instance
     * @param controller updated external controller
     */
    public OFAgentEvent(OFAgentEvent.Type type, OFAgent ofAgent, OFController controller) {
        super(type, ofAgent);
        this.controller = controller;
    }

    /**
     * Returns the updated controller.
     *
     * @return updated controller; null if the event is not controller related
     */
    public OFController controller() {
        return this.controller;
    }
}
