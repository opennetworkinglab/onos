/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.region;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.Annotated;

import java.util.List;
import java.util.Set;

/**
 * Representation of a group of devices located in a common physical or
 * logical region. Optionally, devices in the region can share the same
 * cluster nodes mastership affinities.
 */
public interface Region extends Annotated {

    /**
     * Coarse representation of the type of the region.
     */
    enum Type {
        /**
         * Region represents an entire continent.
         */
        CONTINENT,

        /**
         * Region represents an entire country.
         */
        COUNTRY,

        /**
         * Region represents a metropolitan area.
         */
        METRO,

        /**
         * Region represents a campus.
         */
        CAMPUS,

        /**
         * Region represents a building.
         */
        BUILDING,

        /**
         * Region represents a data center.
         */
        DATA_CENTER,

        /**
         * Region represents a building floor.
         */
        FLOOR,

        /**
         * Region represents a room.
         */
        ROOM,

        /**
         * Region represents a rack.
         */
        RACK,

        /**
         * Region represents a logical grouping.
         */
        LOGICAL_GROUP
    }

    /**
     * Returns the unique identifier of the region.
     *
     * @return region identifier
     */
    RegionId id();

    /**
     * Returns the friendly region name that can be used for display purposes.
     *
     * @return friendly name of the region
     */
    String name();

    /**
     * Returns the region type.
     *
     * @return region type
     */
    Type type();

    /**
     * Returns the list of master node sets. The sets of cluster node identifiers
     * should be listed in the order of preferred mastership. Nodes specified
     * in each sets should be considered with equally priority and devices in
     * the region can be balanced between them based on other criteria, e.g. load.
     *
     * @return list of preferred master node sets
     */
    List<Set<NodeId>> masters();

}
