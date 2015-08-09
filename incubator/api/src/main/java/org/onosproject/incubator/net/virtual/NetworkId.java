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
package org.onosproject.incubator.net.virtual;

import com.google.common.annotations.Beta;

import java.util.Objects;

/**
 * Representation of network identity.
 */
@Beta
public final class NetworkId {

    /**
     * Represents no network, or an unspecified network.
     */
    public static final NetworkId NONE = networkId(-1L);

    /**
     * Represents the underlying physical network.
     */
    public static final NetworkId PHYSICAL = networkId(0L);


    private final long id;

    // Public construction is prohibited
    private NetworkId(long id) {
        this.id = id;
    }


    // Default constructor for serialization
    protected NetworkId() {
        this.id = -1;
    }

    /**
     * Creates a network id using the supplied backing id.
     *
     * @param id network id
     * @return network identifier
     */
    public static NetworkId networkId(long id) {
        return new NetworkId(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetworkId) {
            final NetworkId that = (NetworkId) obj;
            return this.getClass() == that.getClass() && this.id == that.id;
        }
        return false;
    }

    @Override
    public String toString() {
        return Long.toString(id);
    }

}
