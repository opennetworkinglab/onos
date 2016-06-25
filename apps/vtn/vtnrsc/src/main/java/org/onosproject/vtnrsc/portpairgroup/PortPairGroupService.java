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
package org.onosproject.vtnrsc.portpairgroup;

import org.onosproject.event.ListenerService;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;

/**
 * Service for interacting with the inventory of port pair groups.
 */
public interface PortPairGroupService extends ListenerService<PortPairGroupEvent, PortPairGroupListener> {

    /**
     * Returns if the port pair group is existed.
     *
     * @param portPairGroupId port pair group identifier
     * @return true or false if one with the given identifier exists.
     */
    boolean exists(PortPairGroupId portPairGroupId);

    /**
     * Returns the number of port pair groups known to the system.
     *
     * @return number of port pair groups.
     */
    int getPortPairGroupCount();

    /**
     * Returns an iterable collection of the currently known port pair groups.
     *
     * @return collection of port pair groups.
     */
    Iterable<PortPairGroup> getPortPairGroups();

    /**
     * Returns the portPairGroup with the given identifier.
     *
     * @param portPairGroupId port pair group identifier
     * @return PortPairGroup or null if port pair group with the given identifier is not
     *         known.
     */
    PortPairGroup getPortPairGroup(PortPairGroupId portPairGroupId);

    /**
     * Creates a PortPairGroup in the store.
     *
     * @param portPairGroup the port pair group to create
     * @return true if given port pair group is created successfully.
     */
    boolean createPortPairGroup(PortPairGroup portPairGroup);

    /**
     * Updates the portPairGroup in the store.
     *
     * @param portPairGroup the port pair group to update
     * @return true if given port pair group is updated successfully.
     */
    boolean updatePortPairGroup(PortPairGroup portPairGroup);

    /**
     * Deletes portPairGroup by given portPairGroupId.
     *
     * @param portPairGroupId id of port pair group to remove
     * @return true if the give port pair group is deleted successfully.
     */
    boolean removePortPairGroup(PortPairGroupId portPairGroupId);
}
