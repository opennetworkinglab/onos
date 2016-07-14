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

package org.onosproject.segmentrouting;

import java.util.List;

/**
 * Tunnel interface.
 */
public interface Tunnel {

    /**
     * Returns the tunnel ID.
     *
     * @return tunnel ID
     */
    String id();

    /**
     * Returns Segment IDs for the tunnel including source and destination.
     *
     * @return List of Node ID
     */
    List<Integer> labelIds();

    /**
     * Returns the group ID for the tunnel.
     *
     * @return group ID
     */
    int groupId();

    /**
     * Sets group ID for the tunnel.
     *
     * @param groupId group identifier
     */
    void setGroupId(int groupId);

    /**
     * Sets the flag to allow to remove the group or not.
     *
     * @param ok the flag; true - allow to remove
     */
    void allowToRemoveGroup(boolean ok);

    /**
     * Checks if it is allowed to remove the group for the tunnel.
     *
     * @return true if allowed, false otherwise
     */
    boolean isAllowedToRemoveGroup();
}
