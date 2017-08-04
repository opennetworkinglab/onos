/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onlab.util.Identifier;

import java.util.Objects;

/**
 * Representation of network identity.
 */
@Beta
public final class NetworkId extends Identifier<Long> {

    /**
     * Represents no network, or an unspecified network.
     */
    public static final NetworkId NONE = networkId(-1L);

    /**
     * Represents the underlying physical network.
     */
    public static final NetworkId PHYSICAL = networkId(0L);

    /**
     * Checks if the id is for virtual network.
     *
     * @return true if the id is for virtual network.
     */
    public final boolean isVirtualNetworkId() {
        return (!Objects.equals(this, NONE) && !Objects.equals(this, PHYSICAL));
    }

    // Public construction is prohibited
    private NetworkId(long id) {
        super(id);
    }


    // Default constructor for serialization
    protected NetworkId() {
        super(-1L);
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
}
