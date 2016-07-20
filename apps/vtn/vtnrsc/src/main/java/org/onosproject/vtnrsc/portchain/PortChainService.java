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

import org.onosproject.event.ListenerService;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;

/**
 * Service for interacting with the inventory of port chains.
 */
public interface PortChainService extends ListenerService<PortChainEvent, PortChainListener> {

    /**
     * Returns if the port chain is existed.
     *
     * @param portChainId port chain identifier
     * @return true or false if one with the given identifier exists.
     */
    boolean exists(PortChainId portChainId);

    /**
     * Returns the number of port chains known to the system.
     *
     * @return number of port chains.
     */
    int getPortChainCount();

    /**
     * Returns an iterable collection of the currently known port chains.
     *
     * @return collection of port chains.
     */
    Iterable<PortChain> getPortChains();

    /**
     * Returns the portChain with the given identifier.
     *
     * @param portChainId port chain identifier
     * @return PortChain or null if port chain with the given identifier is not
     *         known.
     */
    PortChain getPortChain(PortChainId portChainId);

    /**
     * Creates a PortChain in the store.
     *
     * @param portChain the port chain to create
     * @return true if given port chain is created successfully.
     */
    boolean createPortChain(PortChain portChain);

    /**
     * Updates the portChain in the store.
     *
     * @param portChain the port chain to update
     * @return true if given port chain is updated successfully.
     */
    boolean updatePortChain(PortChain portChain);

    /**
     * Deletes portChain by given portChainId.
     *
     * @param portChainId id of port chain to remove
     * @return true if the give port chain is deleted successfully.
     */
    boolean removePortChain(PortChainId portChainId);
}
