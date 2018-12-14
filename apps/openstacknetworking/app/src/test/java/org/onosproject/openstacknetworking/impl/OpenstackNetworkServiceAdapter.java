/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;

import java.util.Set;

/**
 * Test adapter for OpenstackNetworkService.
 */
public class OpenstackNetworkServiceAdapter implements OpenstackNetworkService {
    @Override
    public Network network(String networkId) {
        return null;
    }

    @Override
    public Set<Network> networks() {
        return ImmutableSet.of();
    }

    @Override
    public Subnet subnet(String subnetId) {
        return null;
    }

    @Override
    public Set<Subnet> subnets() {
        return ImmutableSet.of();
    }

    @Override
    public Set<Subnet> subnets(String networkId) {
        return ImmutableSet.of();
    }

    @Override
    public Port port(String portId) {
        return null;
    }

    @Override
    public Port port(org.onosproject.net.Port port) {
        return null;
    }

    @Override
    public Set<Port> ports() {
        return ImmutableSet.of();
    }

    @Override
    public Set<Port> ports(String networkId) {
        return ImmutableSet.of();
    }

    @Override
    public Set<IpPrefix> getFixedIpsByNetworkType(String type) {
        return null;
    }

    @Override
    public MacAddress externalPeerRouterMac(ExternalGateway externalGateway) {
        return null;
    }

    @Override
    public ExternalPeerRouter externalPeerRouter(IpAddress ipAddress) {
        return null;
    }

    @Override
    public ExternalPeerRouter externalPeerRouter(ExternalGateway externalGateway) {
        return null;
    }

    @Override
    public Set<ExternalPeerRouter> externalPeerRouters() {
        return ImmutableSet.of();
    }

    @Override
    public IpPrefix ipPrefix(String portId) {
        return null;
    }

    @Override
    public Type networkType(String netId) {
        return null;
    }

    @Override
    public String gatewayIp(String portId) {
        return null;
    }

    @Override
    public String segmentId(String netId) {
        return null;
    }

    @Override
    public void addListener(OpenstackNetworkListener listener) {

    }

    @Override
    public void removeListener(OpenstackNetworkListener listener) {

    }
}
