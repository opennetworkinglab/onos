/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.flow;

import org.onosproject.event.AbstractEvent;

/**
 * Describes flow rule event.
 */
public class FlowRuleEvent extends AbstractEvent<FlowRuleEvent.Type, FlowRule> {

    /**
     * Type of flow rule events.
     */
    public enum Type {
        /**
         * Signifies that a new flow rule has been detected.
         */
        RULE_ADDED,

        /**
         * Signifies that a flow rule has been removed.
         */
        RULE_REMOVED,

        /**
         * Signifies that a rule has been updated.
         */
        RULE_UPDATED,

        // internal event between Manager <-> Store

        /*
         * Signifies that a request to add flow rule has been added to the store.
         */
        RULE_ADD_REQUESTED,
        /*
         * Signifies that a request to remove flow rule has been added to the store.
         */
        RULE_REMOVE_REQUESTED,
    }

    /**
     * Creates an event of a given type and for the specified flow rule and the
     * current time.
     *
     * @param type     flow rule event type
     * @param flowRule event flow rule subject
     */
    public FlowRuleEvent(Type type, FlowRule flowRule) {
        super(type, flowRule);
    }

    /**
     * Creates an event of a given type and for the specified flow rule and time.
     *
     * @param type     flow rule event type
     * @param flowRule event flow rule subject
     * @param time     occurrence time
     */
    public FlowRuleEvent(Type type, FlowRule flowRule, long time) {
        super(type, flowRule, time);
    }

}
