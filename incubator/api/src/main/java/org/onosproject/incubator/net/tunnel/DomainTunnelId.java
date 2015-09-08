/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.incubator.net.tunnel;

/**
 * A wrapper class for a long used to identify domain level tunnels.
 */
public final class DomainTunnelId {

    private final long value;

    /**
     * Creates a tunnel identifier from the specified tunnel.
     *
     * @param value long value
     * @return domain tunnel identifier
     */
    public static DomainTunnelId valueOf(long value) {
        return new DomainTunnelId(value);
    }

    /**
     * Creates a tunnel identifier from the specified tunnel.
     *
     * @param value long value as a string
     * @return domain tunnel identifier
     */
    public static DomainTunnelId valueOf(String value) {
        return new DomainTunnelId(Long.parseLong(value));
    }

    /**
     * Constructor for serializer.
     */
    protected DomainTunnelId() {
        this.value = 0;
    }

    /**
     * Constructs the Domain ID corresponding to a given long value.
     *
     * @param value the underlying value of this domain ID
     */
    public DomainTunnelId(long value) {
        this.value = value;
    }

    /**
     * Returns the backing value of this domain ID.
     *
     * @return the long value
     */
    public long id() {
        return value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DomainTunnelId)) {
            return false;
        }
        DomainTunnelId that = (DomainTunnelId) obj;
        return this.value == that.value;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(value);
    }
}
