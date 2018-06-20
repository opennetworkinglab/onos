/*
 * Copyright 2018-present Open Networking Foundation
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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents two devices that are paired by configuration. An EdgePair for
 * (dev1, dev2) is the same as as EdgePair for (dev2, dev1)
 */
public final class EdgePair {
    DeviceId dev1;
    DeviceId dev2;

    EdgePair(DeviceId dev1, DeviceId dev2) {
        this.dev1 = dev1;
        this.dev2 = dev2;
    }

    boolean includes(DeviceId dev) {
        return dev1.equals(dev) || dev2.equals(dev);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EdgePair)) {
            return false;
        }
        EdgePair that = (EdgePair) o;
        return ((this.dev1.equals(that.dev1) && this.dev2.equals(that.dev2)) ||
                (this.dev1.equals(that.dev2) && this.dev2.equals(that.dev1)));
    }

    @Override
    public int hashCode() {
        if (dev1.toString().compareTo(dev2.toString()) <= 0) {
            return Objects.hash(dev1, dev2);
        } else {
            return Objects.hash(dev2, dev1);
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("Dev1", dev1)
                .add("Dev2", dev2)
                .toString();
    }
}