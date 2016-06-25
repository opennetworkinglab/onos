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
package org.onosproject.vtnrsc.portpair;

import org.onosproject.event.ListenerService;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
/**
 * Service for interacting with the inventory of port pairs.
 */
public interface PortPairService extends ListenerService<PortPairEvent, PortPairListener> {

    /**
     * Returns if the port pair is existed.
     *
     * @param portPairId port pair identifier
     * @return true or false if one with the given identifier exists.
     */
    boolean exists(PortPairId portPairId);

    /**
     * Returns the number of port pairs known to the system.
     *
     * @return number of port pairs.
     */
    int getPortPairCount();

    /**
     * Returns an iterable collection of the currently known port pairs.
     *
     * @return collection of port pairs.
     */
    Iterable<PortPair> getPortPairs();

    /**
     * Returns the portPair with the given identifier.
     *
     * @param portPairId port pair identifier
     * @return PortPair or null if port pair with the given identifier is not
     *         known.
     */
    PortPair getPortPair(PortPairId portPairId);

    /**
     * Creates a PortPair in the store.
     *
     * @param portPair the port pair to create
     * @return true if given port pair is created successfully.
     */
    boolean createPortPair(PortPair portPair);

    /**
     * Updates the portPair in the store.
     *
     * @param portPair the port pair to update
     * @return true if given port pair is updated successfully.
     */
    boolean updatePortPair(PortPair portPair);

    /**
     * Deletes portPair by given portPairId.
     *
     * @param portPairId id of port pair to remove
     * @return true if the give port pair is deleted successfully.
     */
    boolean removePortPair(PortPairId portPairId);
}
