/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.ovsdb.controller;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * The class representing an ovsdb tunnel.
 * This class is immutable.
 */
public final class OvsdbTunnel {

    private final IpAddress localIp;
    private final IpAddress remoteIp;

    public enum Type {
        VXLAN, GRE
    }

    private final Type tunnelType;
    private final OvsdbTunnelName tunnelName;

    /**
     * Constructor from an IpAddress localIp, IpAddress remoteIp Type tunnelType,
     * OvsdbTunnelName tunnelName.
     *
     * @param localIp the localIp to use
     * @param remoteIp the remoteIp to use
     * @param tunnelType the tunnelType to use
     * @param tunnelName the tunnelName to use
     */
    public OvsdbTunnel(IpAddress localIp, IpAddress remoteIp, Type tunnelType,
                       OvsdbTunnelName tunnelName) {
        checkNotNull(localIp, "portName is not null");
        checkNotNull(remoteIp, "portName is not null");
        checkNotNull(tunnelName, "portName is not null");
        this.localIp = localIp;
        this.remoteIp = remoteIp;
        this.tunnelType = tunnelType;
        this.tunnelName = tunnelName;
    }

    /**
     * Gets the local IP of tunnel.
     *
     * @return the local IP of tunnel
     */
    public IpAddress localIp() {
        return localIp;
    }

    /**
     * Gets the remote IP of tunnel.
     *
     * @return the remote IP of tunnel
     */
    public IpAddress remoteIp() {
        return remoteIp;
    }

    /**
     * Gets the tunnel type of tunnel.
     *
     * @return the tunnel type of tunnel
     */
    public Type tunnelType() {
        return tunnelType;
    }

    /**
     * Gets the tunnel name of tunnel.
     *
     * @return the tunnel name of tunnel
     */
    public OvsdbTunnelName tunnelName() {
        return tunnelName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(localIp, remoteIp, tunnelType, tunnelName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbTunnel) {
            final OvsdbTunnel otherOvsdbTunnel = (OvsdbTunnel) obj;
            return Objects.equals(this.localIp, otherOvsdbTunnel.localIp)
                    && Objects.equals(this.remoteIp, otherOvsdbTunnel.remoteIp)
                    && Objects.equals(this.tunnelType,
                                      otherOvsdbTunnel.tunnelType)
                    && Objects.equals(this.tunnelName,
                                      otherOvsdbTunnel.tunnelName);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("localIp", localIp.toString())
                .add("remoteIp", remoteIp.toString())
                .add("tunnelType", tunnelType).add("tunnelName", tunnelName)
                .toString();
    }
}
