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
package org.onosproject.routing.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTools;
import org.onosproject.net.PortNumber;

import java.util.Objects;

/**
 * Represents an address of a {@link BgpSpeaker} configured on an
 * {@link Interface}.
 * <p>
 * Each InterfaceAddress includes the interface name and an IP address.
 * </p>
 */
public class InterfaceAddress {
    private final ConnectPoint connectPoint;
    private final IpAddress ipAddress;

    /**
     * Creates an InterfaceAddress object.
     *
     * @param dpid the DPID of the interface as a String
     * @param port the port of the interface
     * @param ipAddress the IP address of a {@link BgpSpeaker} configured on
     * the interface
     */
    public InterfaceAddress(@JsonProperty("interfaceDpid") String dpid,
                            @JsonProperty("interfacePort") int port,
                            @JsonProperty("ipAddress") String ipAddress) {
        this.connectPoint = new ConnectPoint(
                DeviceId.deviceId(NetTools.dpidToUri(dpid)),
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
     * Gets the IP address of a BGP speaker configured on an {@link Interface}.
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

        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }

        InterfaceAddress that = (InterfaceAddress) obj;
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
