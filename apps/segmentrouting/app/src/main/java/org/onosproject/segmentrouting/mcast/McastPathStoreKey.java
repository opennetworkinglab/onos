/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.segmentrouting.mcast;

import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Key of multicast path store.
 */
public class McastPathStoreKey {
    // Identify path using group address and source
    private final IpAddress mcastIp;
    private final ConnectPoint source;

    /**
     * Constructs the key of multicast path store.
     *
     * @param mcastIp multicast group IP address
     * @param source source connect point
     */
    public McastPathStoreKey(IpAddress mcastIp, ConnectPoint source) {
        checkNotNull(mcastIp, "mcastIp cannot be null");
        checkNotNull(source, "source cannot be null");
        checkArgument(mcastIp.isMulticast(), "mcastIp must be a multicast address");
        this.mcastIp = mcastIp;
        this.source = source;
    }

    // Constructor for serialization
    private McastPathStoreKey() {
        this.mcastIp = null;
        this.source = null;
    }

    /**
     * Returns the multicast IP address of this key.
     *
     * @return multicast IP
     */
    public IpAddress mcastIp() {
        return mcastIp;
    }

    /**
     * Returns the device ID of this key.
     *
     * @return device ID
     */
    public ConnectPoint source() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof McastPathStoreKey)) {
            return false;
        }
        McastPathStoreKey that =
                (McastPathStoreKey) o;
        return (Objects.equals(this.mcastIp, that.mcastIp) &&
                Objects.equals(this.source, that.source));
    }

    @Override
    public int hashCode() {
         return Objects.hash(mcastIp, source);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("mcastIp", mcastIp)
                .add("source", source)
                .toString();
    }
}
