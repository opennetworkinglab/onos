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

package org.onosproject.routing;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier for a next hop group.
 */
public class NextHopGroupKey {

    private final IpAddress address;

    /**
     * Creates a new next hop group key.
     *
     * @param address next hop's IP address
     */
    public NextHopGroupKey(IpAddress address) {
        this.address = checkNotNull(address);
    }

    /**
     * Returns the next hop's IP address.
     *
     * @return next hop's IP address
     */
    public IpAddress address() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NextHopGroupKey)) {
            return false;
        }

        NextHopGroupKey that = (NextHopGroupKey) o;

        return Objects.equals(this.address, that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("address", address)
                .toString();
    }
}
