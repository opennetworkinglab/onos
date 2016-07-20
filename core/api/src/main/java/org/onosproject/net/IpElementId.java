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
package org.onosproject.net;

import java.util.Objects;
import org.onlab.packet.IpAddress;
import com.google.common.base.MoreObjects;

/**
 * Represent for a Element ID using ip address.
 */
public final class IpElementId extends ElementId {

    private final IpAddress ipAddress;

    /**
     * Public construction is prohibited.
     * @param ipAddress ip address
     */
    private IpElementId(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Create a IP Element ID.
     * @param ipAddress IP address
     * @return IpElementId
     */
    public static IpElementId ipElement(IpAddress ipAddress) {
        return new IpElementId(ipAddress);
    }

    /**
     * Returns the ip address.
     *
     * @return ipAddress
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IpElementId) {
            final IpElementId other = (IpElementId) obj;
            return Objects.equals(this.ipAddress, other.ipAddress);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("ipAddress", ipAddress).toString();
    }
}
