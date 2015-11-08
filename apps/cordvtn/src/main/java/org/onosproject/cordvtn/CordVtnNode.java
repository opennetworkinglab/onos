/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;

import java.util.Comparator;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a compute infrastructure node for CORD VTN service.
 */
public final class CordVtnNode {

    private final String hostname;
    private final IpAddress ovsdbIp;
    private final TpPort ovsdbPort;
    private final DeviceId bridgeId;

    public static final Comparator<CordVtnNode> CORDVTN_NODE_COMPARATOR =
            (node1, node2) -> node1.hostname().compareTo(node2.hostname());

    /**
     * Creates a new node.
     *
     * @param hostname hostname
     * @param ovsdbIp OVSDB server IP address
     * @param ovsdbPort OVSDB server port number
     * @param bridgeId integration bridge identifier
     */
    public CordVtnNode(String hostname, IpAddress ovsdbIp, TpPort ovsdbPort, DeviceId bridgeId) {
        this.hostname = checkNotNull(hostname);
        this.ovsdbIp = checkNotNull(ovsdbIp);
        this.ovsdbPort = checkNotNull(ovsdbPort);
        this.bridgeId = checkNotNull(bridgeId);
    }

    /**
     * Returns the OVSDB server IP address.
     *
     * @return ip address
     */
    public IpAddress ovsdbIp() {
        return this.ovsdbIp;
    }

    /**
     * Returns the OVSDB server port number.
     *
     * @return port number
     */
    public TpPort ovsdbPort() {
        return this.ovsdbPort;
    }

    /**
     * Returns the hostname.
     *
     * @return hostname
     */
    public String hostname() {
        return this.hostname;
    }

    /**
     * Returns the identifier of the integration bridge.
     *
     * @return device id
     */
    public DeviceId intBrId() {
        return this.bridgeId;
    }

    /**
     * Returns the identifier of the OVSDB device.
     *
     * @return device id
     */
    public DeviceId ovsdbId() {
        return DeviceId.deviceId("ovsdb:" + this.ovsdbIp.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof CordVtnNode) {
            CordVtnNode that = (CordVtnNode) obj;
            if (Objects.equals(hostname, that.hostname) &&
                    Objects.equals(ovsdbIp, that.ovsdbIp) &&
                    Objects.equals(ovsdbPort, that.ovsdbPort) &&
                    Objects.equals(bridgeId, that.bridgeId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, ovsdbIp, ovsdbPort);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("host", hostname)
                .add("ip", ovsdbIp)
                .add("port", ovsdbPort)
                .add("bridgeId", bridgeId)
                .toString();
    }
}
