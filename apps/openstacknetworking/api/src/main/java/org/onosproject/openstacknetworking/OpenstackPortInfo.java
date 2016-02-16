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
package org.onosproject.openstacknetworking;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contains OpenstackPort Information.
 */
public class OpenstackPortInfo {
    private final Ip4Address hostIp;
    private final MacAddress hostMac;
    private final DeviceId deviceId;
    private final long vni;
    private final Ip4Address gatewayIP;

    public OpenstackPortInfo(Ip4Address hostIp, MacAddress hostMac, DeviceId deviceId, long vni, Ip4Address gatewayIP) {
        this.hostIp = hostIp;
        this.hostMac = hostMac;
        this.deviceId = deviceId;
        this.vni = vni;
        this.gatewayIP = gatewayIP;
    }

    public Ip4Address ip() {
        return hostIp;
    }

    public MacAddress mac() {
        return hostMac;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public long vni() {
        return vni;
    }

    public Ip4Address gatewayIP() {
        return gatewayIP;
    }

    public static OpenstackPortInfo.Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Ip4Address hostIp;
        private MacAddress hostMac;
        private DeviceId deviceId;
        private long vni;
        private Ip4Address gatewayIP;

        public Builder setGatewayIP(Ip4Address gatewayIP) {
            this.gatewayIP = checkNotNull(gatewayIP, "gatewayIP cannot be null");
            return this;
        }

        public Builder setHostIp(Ip4Address hostIp) {
            this.hostIp = checkNotNull(hostIp, "hostIp cannot be null");
            return this;
        }

        public Builder setHostMac(MacAddress hostMac) {
            this.hostMac = checkNotNull(hostMac, "hostMac cannot be bull");
            return this;
        }

        public Builder setDeviceId(DeviceId deviceId) {
            this.deviceId = checkNotNull(deviceId, "deviceId cannot be null");
            return this;
        }

        public Builder setVni(long vni) {
            this.vni = checkNotNull(vni, "vni cannot be null");
            return this;
        }
        public OpenstackPortInfo build() {
            return new OpenstackPortInfo(this);
        }
    }

    private OpenstackPortInfo(Builder builder) {
        hostIp = builder.hostIp;
        hostMac = builder.hostMac;
        deviceId = builder.deviceId;
        vni = builder.vni;
        gatewayIP = builder.gatewayIP;
    }
}
