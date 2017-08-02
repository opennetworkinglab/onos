/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onosproject.dhcprelay;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.BasePacket;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.dhcprelay.api.DhcpHandler;
import org.onosproject.dhcprelay.config.DhcpServerConfig;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.packet.PacketContext;

import java.util.Collection;
import java.util.Optional;

@Component
@Service
@Property(name = "version", value = "6")
public class Dhcp6HandlerImpl implements DhcpHandler {

    @Override
    public void processDhcpPacket(PacketContext context, BasePacket dhcp6Payload) {

    }

    @Override
    public Optional<IpAddress> getDhcpServerIp() {
        return null;
    }

    @Override
    public Optional<IpAddress> getDhcpGatewayIp() {
        return null;
    }

    @Override
    public Optional<MacAddress> getDhcpConnectMac() {
        return null;
    }

    @Override
    public void setDhcpGatewayIp(IpAddress dhcpGatewayIp) {

    }

    @Override
    public void setDhcpConnectVlan(VlanId dhcpConnectVlan) {

    }

    @Override
    public void setDhcpConnectMac(MacAddress dhcpConnectMac) {

    }

    @Override
    public void setDhcpServerConnectPoint(ConnectPoint dhcpServerConnectPoint) {

    }

    @Override
    public void setDhcpServerIp(IpAddress dhcpServerIp) {

    }

    @Override
    public void setDefaultDhcpServerConfigs(Collection<DhcpServerConfig> configs) {

    }

    @Override
    public void setIndirectDhcpServerConfigs(Collection<DhcpServerConfig> configs) {

    }
}
