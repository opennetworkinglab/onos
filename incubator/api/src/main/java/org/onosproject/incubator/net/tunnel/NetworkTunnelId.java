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

package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

/**
 * Representation of a Network Tunnel Id.
 */
@Beta
public final class NetworkTunnelId extends Identifier<Long> {
    /**
     * Creates an tunnel identifier from the specified tunnel.
     *
     * @param value long value
     * @return tunnel identifier
     */
    public static NetworkTunnelId valueOf(long value) {
        return new NetworkTunnelId(value);
    }

    public static NetworkTunnelId valueOf(String value) {
        return new NetworkTunnelId(Long.parseLong(value));
    }

    /**
     * Constructor for serializer.
     */
    NetworkTunnelId() {
        super(0L);
    }

    /**
     * Constructs the ID corresponding to a given long value.
     *
     * @param value the underlying value of this ID
     */
    public NetworkTunnelId(long value) {
        super(value);
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(identifier);
    }
}
