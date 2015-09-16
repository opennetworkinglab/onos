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

import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * OvsdbNode implementation.
 */
public class DefaultOvsdbNode implements OvsdbNode {

    private final String hostname;
    private final IpAddress ip;
    private final TpPort port;
    private final DeviceId deviceId;
    private final DeviceId bridgeId;
    private final State state;

    public DefaultOvsdbNode(String hostname, IpAddress ip, TpPort port,
                            DeviceId bridgeId, State state) {
        this.hostname = hostname;
        this.ip = ip;
        this.port = port;
        this.deviceId = DeviceId.deviceId(
                "ovsdb:" + ip.toString() + ":" + port.toString());
        this.bridgeId = bridgeId;
        this.state = state;
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
    public String hostname() {
        return this.hostname;
    }

    @Override
    public State state() {
        return this.state;
    }

    @Override
    public DeviceId deviceId() {
        return this.deviceId;
    }

    @Override
    public DeviceId bridgeId() {
        return this.bridgeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DefaultOvsdbNode) {
            DefaultOvsdbNode that = (DefaultOvsdbNode) o;
            // We compare the ip and port only.
            if (this.ip.equals(that.ip) && this.port.equals(that.port)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
