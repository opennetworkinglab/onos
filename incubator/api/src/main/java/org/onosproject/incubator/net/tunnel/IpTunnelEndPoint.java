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

import java.util.Objects;

import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;

import com.google.common.base.MoreObjects;

/**
 * Represent for a tunnel point using ip address.
 */
@Beta
public final class IpTunnelEndPoint implements TunnelEndPoint {

    private final IpAddress ip;

    /**
     * Public construction is prohibited.
     * @param ip ip address
     */
    private IpTunnelEndPoint(IpAddress ip) {
        this.ip = ip;
    }

    /**
     * Create a IP tunnel end point.
     * @param ip IP address
     * @return IpTunnelEndPoint
     */
    public static IpTunnelEndPoint ipTunnelPoint(IpAddress ip) {
        return new IpTunnelEndPoint(ip);
    }

    /**
     * Returns IP address.
     * @return IP address
     */
    public IpAddress ip() {
        return ip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IpTunnelEndPoint) {
            final IpTunnelEndPoint other = (IpTunnelEndPoint) obj;
            return Objects.equals(this.ip, other.ip);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("ip", ip).toString();
    }
}
