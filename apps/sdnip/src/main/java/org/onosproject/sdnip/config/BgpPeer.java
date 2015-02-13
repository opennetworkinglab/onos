/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip.config;

import java.util.Objects;

import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Configuration details for a BGP peer.
 */
public class BgpPeer {
    private final ConnectPoint connectPoint;
    private final IpAddress ipAddress;

    /**
     * Creates a new BgpPeer.
     *
     * @param dpid the DPID of the switch the peer is attached at, as a String
     * @param port the port the peer is attached at
     * @param ipAddress the IP address of the peer as a String
     */
    public BgpPeer(@JsonProperty("attachmentDpid") String dpid,
                   @JsonProperty("attachmentPort") int port,
                   @JsonProperty("ipAddress") String ipAddress) {
        this.connectPoint = new ConnectPoint(
                DeviceId.deviceId(SdnIpConfigurationReader.dpidToUri(dpid)),
                PortNumber.portNumber(port));
        this.ipAddress = IpAddress.valueOf(ipAddress);
    }

    /**
     * Gets the connection point of the peer.
     *
     * @return the connection point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    /**
     * Gets the IP address of the peer.
     *
     * @return the IP address
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, ipAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof BgpPeer)) {
            return false;
        }

        BgpPeer that = (BgpPeer) obj;
        return Objects.equals(this.connectPoint, that.connectPoint)
                && Objects.equals(this.ipAddress, that.ipAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("connectPoint", connectPoint)
                .add("ipAddress", ipAddress)
                .toString();
    }
}
