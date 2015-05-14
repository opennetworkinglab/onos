/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onosproject.net.DeviceId;

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
     * Creates a tunnel.
     *
     * @return true if succeeds, false otherwise
     */
    boolean create();

    /**
     * Removes the tunnel.
     *
     * @return true if succeeds, false otherwise.
     */
    boolean remove();

    /**
     * Returns the group ID for the tunnel.
     *
     * @return group ID
     */
    int groupId();

    /**
     * Returns the source device Id of the tunnel.
     *
     * @return source device Id
     */
    DeviceId source();
}
