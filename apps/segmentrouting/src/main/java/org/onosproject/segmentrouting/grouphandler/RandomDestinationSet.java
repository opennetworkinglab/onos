/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.segmentrouting.grouphandler;

import org.onosproject.net.DeviceId;

/**
 * Extends its super class modifying its internal behavior.
 * Pick a neighbor will pick a random neighbor.
 */
public class RandomDestinationSet extends DestinationSet {

    public RandomDestinationSet(DeviceId dstSw) {
        super(true, dstSw);
    }

    public RandomDestinationSet(int edgeLabel,
                             DeviceId dstSw) {
        super(true, edgeLabel, dstSw);
    }

    public RandomDestinationSet() {
        super();
    }

    // XXX revisit the need for this class since neighbors no longer stored here
    // will be handled when we fix pseudowires for dual-Tor scenarios


    /*@Override
    public DeviceId getFirstNeighbor() {
        if (getDeviceIds().isEmpty()) {
            return DeviceId.NONE;
        }
        int size = getDeviceIds().size();
        int index = RandomUtils.nextInt(0, size);
        return Iterables.get(getDeviceIds(), index);
    }*/

    @Override
    public String toString() {
        return " RandomNeighborset Sw: " //+ getDeviceIds()
                + " and Label: " //+ getEdgeLabel()
                + " for destination: "; // + getDestinationSw();
    }
}
