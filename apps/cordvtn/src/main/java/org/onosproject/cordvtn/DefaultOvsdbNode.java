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

import java.util.Objects;

/**
 * OvsdbNode implementation.
 */
public class DefaultOvsdbNode implements OvsdbNode {

    private final String host;
    private final IpAddress ip;
    private final TpPort port;
    private final DeviceId brId;

    public DefaultOvsdbNode(String host, IpAddress ip, TpPort port, DeviceId brId) {
        this.host = host;
        this.ip = ip;
        this.port = port;
        this.brId = brId;
    }

    @Override
    public IpAddress ip() {
        return this.ip;
    }

    @Override
    public TpPort port() {
        return this.port;
    }

    @Override
    public String host() {
        return this.host;
    }

    @Override
    public DeviceId intBrId() {
        return this.brId;
    }

    @Override
    public DeviceId deviceId() {
        return DeviceId.deviceId("ovsdb:" + this.ip.toString() + ":" + this.port.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DefaultOvsdbNode) {
            DefaultOvsdbNode that = (DefaultOvsdbNode) o;
            if (this.host.equals(that.host) &&
                    this.ip.equals(that.ip) &&
                    this.port.equals(that.port) &&
                    this.brId.equals(that.brId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, ip, port);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("host", host)
                .add("ip", ip)
                .add("port", port)
                .add("bridgeId", brId)
                .toString();
    }
}
