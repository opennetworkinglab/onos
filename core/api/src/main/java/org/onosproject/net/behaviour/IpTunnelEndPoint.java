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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;

/**
 * Represent for a tunnel point using ip address.
 *
 * @deprecated version 1.7.0 - Hummingbird; use {@code TunnelEndPoint<IpAddress>}
 */
@Deprecated
@Beta
public final class IpTunnelEndPoint extends TunnelEndPoint<IpAddress> {

    /**
     * Public construction is prohibited.
     * @param ip ip address
     */
    private IpTunnelEndPoint(IpAddress ip) {
        super(ip);
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
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("ip", value).toString();
    }
}
