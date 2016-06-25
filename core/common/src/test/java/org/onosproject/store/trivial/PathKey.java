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
package org.onosproject.store.trivial;

import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Key for filing pre-computed paths between source and destination devices.
 */
class PathKey {
    private final DeviceId src;
    private final DeviceId dst;

    /**
     * Creates a path key from the given source/dest pair.
     * @param src source device
     * @param dst destination device
     */
    PathKey(DeviceId src, DeviceId dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PathKey) {
            final PathKey other = (PathKey) obj;
            return Objects.equals(this.src, other.src) && Objects.equals(this.dst, other.dst);
        }
        return false;
    }
}
