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

import com.google.common.collect.ImmutableSet;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Describes region event.
 */
public class RegionEvent extends AbstractEvent<RegionEvent.Type, Region> {

    private final Set<DeviceId> deviceIds;

    public enum Type {
        /**
         * Signifies that a new region was created.
         */
        REGION_ADDED,

        /**
         * Signifies that a region was removed.
         */
        REGION_REMOVED,

        /**
         * Signifies that a region was updated.
         */
        REGION_UPDATED,

        /**
         * Signifies that a region device membership has changed.
         */
        REGION_MEMBERSHIP_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified region and the
     * current time.
     *
     * @param type   device event type
     * @param region event region subject
     */
    public RegionEvent(Type type, Region region) {
        this(type, region, null);
    }

    /**
     * Creates an event of a given type and for the specified region, device
     * id list and the current time.
     *
     * @param type      device event type
     * @param region    event region subject
     * @param deviceIds optional set of device ids
     */
    public RegionEvent(Type type, Region region, Set<DeviceId> deviceIds) {
        super(type, region);
        this.deviceIds = deviceIds != null ? ImmutableSet.copyOf(deviceIds) : ImmutableSet.of();
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type      device event type
     * @param region    event region subject
     * @param deviceIds optional set of device ids
     * @param time      occurrence time
     */
    public RegionEvent(Type type, Region region, Set<DeviceId> deviceIds, long time) {
        super(type, region, time);
        this.deviceIds = deviceIds != null ? ImmutableSet.copyOf(deviceIds) : ImmutableSet.of();
    }

}
