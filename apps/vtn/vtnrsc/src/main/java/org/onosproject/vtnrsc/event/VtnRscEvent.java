/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.event;

import org.onosproject.event.AbstractEvent;


/**
 * Describes network vtnrsc event.
 */
public class VtnRscEvent
        extends AbstractEvent<VtnRscEvent.Type, VtnRscEventFeedback> {

    /**
     * Type of vtnrsc events.
     */
    public enum Type {
        /**
         * Signifies that floating IP has create.
         */
        FLOATINGIP_PUT,
        /**
         * Signifies that floating IP has delete.
         */
        FLOATINGIP_DELETE,
        /**
         * Signifies that Floating IP has been bound.
         */
        FLOATINGIP_BIND,
        /**
         * Signifies that Floating IP has been unbound.
         */
        FLOATINGIP_UNBIND,
        /**
         * Signifies that router has create.
         */
        ROUTER_PUT,
        /**
         * Signifies that router has delete.
         */
        ROUTER_DELETE,
        /**
         * Signifies that router interface has add.
         */
        ROUTER_INTERFACE_PUT,
        /**
         * Signifies that router interface has remove.
         */
        ROUTER_INTERFACE_DELETE,
        /**
         * Signifies that port-pair has add.
         */
        PORT_PAIR_PUT,
        /**
         * Signifies that port-pair has remove.
         */
        PORT_PAIR_DELETE,
        /**
         * Signifies that port-pair has update.
         */
        PORT_PAIR_UPDATE,
        /**
         * Signifies that port-pair-group has add.
         */
        PORT_PAIR_GROUP_PUT,
        /**
         * Signifies that port-pair-group has remove.
         */
        PORT_PAIR_GROUP_DELETE,
        /**
         * Signifies that port-pair-group has update.
         */
        PORT_PAIR_GROUP_UPDATE,
        /**
         * Signifies that flow-classifier has add.
         */
        FLOW_CLASSIFIER_PUT,
        /**
         * Signifies that flow-classifier has remove.
         */
        FLOW_CLASSIFIER_DELETE,
        /**
         * Signifies that flow-classifier has update.
         */
        FLOW_CLASSIFIER_UPDATE,
        /**
         * Signifies that port-chain has add.
         */
        PORT_CHAIN_PUT,
        /**
         * Signifies that port-chain has remove.
         */
        PORT_CHAIN_DELETE,
        /**
         * Signifies that port-chain has update.
         */
        PORT_CHAIN_UPDATE,
        /**
         * Signifies that virtual-port has created.
         */
        VIRTUAL_PORT_PUT,
        /**
         * Signifies that virtual-port has removed.
         */
        VIRTUAL_PORT_DELETE
    }

    /**
     * Creates an event of a given type and for the specified vtn event feedback.
     *
     * @param type Vtnrsc event type
     * @param vtnFeedback event VtnrscEventFeedback subject
     */
    public VtnRscEvent(Type type, VtnRscEventFeedback vtnFeedback) {
        super(type, vtnFeedback);
    }

    /**
     * Creates an event of a given type and for the specified vtn event feedback.
     *
     * @param type Vtnrsc event type
     * @param vtnFeedback event VtnrscEventFeedback subject
     * @param time occurrence time
     */
    public VtnRscEvent(Type type, VtnRscEventFeedback vtnFeedback, long time) {
        super(type, vtnFeedback, time);
    }
}
