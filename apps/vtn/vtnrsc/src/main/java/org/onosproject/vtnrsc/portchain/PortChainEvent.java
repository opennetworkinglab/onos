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
package org.onosproject.vtnrsc.portchain;

import org.onosproject.event.AbstractEvent;
import org.onosproject.vtnrsc.PortChain;

/**
 * Describes network Port-Chain event.
 */
public class PortChainEvent extends AbstractEvent<PortChainEvent.Type, PortChain> {
    /**
     * Type of port-chain events.
     */
    public enum Type {
        /**
         * Signifies that port-chain has been created.
         */
        PORT_CHAIN_PUT,
        /**
         * Signifies that port-chain has been deleted.
         */
        PORT_CHAIN_DELETE,
        /**
         * Signifies that port-chain has been updated.
         */
        PORT_CHAIN_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified Port-Chain.
     *
     * @param type Port-Chain event type
     * @param portChain Port-Chain subject
     */
    public PortChainEvent(Type type, PortChain portChain) {
        super(type, portChain);
    }

    /**
     * Creates an event of a given type and for the specified Port-Chain.
     *
     * @param type Port-Chain event type
     * @param portChain Port-Chain subject
     * @param time occurrence time
     */
    public PortChainEvent(Type type, PortChain portChain, long time) {
        super(type, portChain, time);
    }
}
