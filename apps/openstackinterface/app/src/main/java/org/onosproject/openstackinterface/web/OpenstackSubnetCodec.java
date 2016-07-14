/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstackinterface.web;

import com.fasterxml.jackson.databind.JsonNode;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onlab.packet.Ip4Address;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encodes and decodes the OpenstackSubnet.
 */
public class OpenstackSubnetCodec extends JsonCodec<OpenstackSubnet> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // JSON Field names
    private static final String SUBNET = "subnet";
    private static final String NAME = "name";
    private static final String ENABLE_DHCP = "enable_dhcp";
    private static final String NETWORK_ID = "network_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String DNS_NAMESERVERS = "dns_nameservers";
    private static final String GATEWAY_IP = "gateway_ip";
    private static final String CIDR = "cidr";
    private static final String ID = "id";

    @Override
    public OpenstackSubnet decode(ObjectNode json, CodecContext context) {
        checkNotNull(json);
        JsonNode subnetInfo = json.get(SUBNET);
        if (subnetInfo == null) {
            subnetInfo = json;
        }

        String name = subnetInfo.path(NAME).asText();
        boolean enableDhcp = subnetInfo.path(ENABLE_DHCP).asBoolean();
        String networkId = subnetInfo.path(NETWORK_ID).asText();
        String tenantId = subnetInfo.path(TENANT_ID).asText();
        ArrayNode dnsNameservsers = (ArrayNode) subnetInfo.path(DNS_NAMESERVERS);
        List<Ip4Address> dnsList = Lists.newArrayList();
        if (dnsNameservsers != null && !dnsNameservsers.isMissingNode()) {
            dnsNameservsers.forEach(dns -> dnsList.add(Ip4Address.valueOf(dns.asText())));
        }
        String gatewayIp = subnetInfo.path(GATEWAY_IP).asText();
        String cidr = subnetInfo.path(CIDR).asText();
        String id = subnetInfo.path(ID).asText();

        OpenstackSubnet openstackSubnet = OpenstackSubnet.builder()
                .setName(name)
                .setEnableDhcp(enableDhcp)
                .setNetworkId(networkId)
                .setTenantId(tenantId)
                .setDnsNameservers(dnsList)
                .setGatewayIp(gatewayIp)
                .setCidr(cidr)
                .setId(id)
                .build();
        return openstackSubnet;
    }
}
