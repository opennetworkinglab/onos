/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.imr.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a path in terms of list of elements that it has to traverse
 * and weight.
 */
public class Path {
    private List<ElementId> path;
    private float weight;

    /**
     * Returns the list of elements composing the path.
     * @return the actual path
     */
    public List<ElementId> path() {
        return path;
    }

    /**
     * Returns the weight related to the path.
     * @return the weight
     */
    public float weight() {
        return weight;
    }

    /**
     * Creates a Path using Jackson from a JSON Object.
     * @param path List of element id representig the path.
     * @param weight Weight related to the path.
     */
    @JsonCreator
    public Path(@JsonProperty("path") List<String> path,
                @JsonProperty("weight") float weight) {
        this.path = new ArrayList<>();

        path.forEach(deviceName -> {
            try {
                this.path.add((HostId.hostId(deviceName)));
            } catch (IllegalArgumentException e) {
                this.path.add(DeviceId.deviceId(deviceName));
            }
        });
        this.weight = weight;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Path", this.path())
                .toString();
    }
}
