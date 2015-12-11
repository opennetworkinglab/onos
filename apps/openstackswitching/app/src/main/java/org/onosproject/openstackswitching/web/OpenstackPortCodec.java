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
package org.onosproject.openstackswitching.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackswitching.OpenstackPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Encodes and decodes the OpenstackPort.
 */
public class OpenstackPortCodec extends JsonCodec<OpenstackPort> {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackPortCodec.class);

    // JSON field names
    private static final String PORT = "port";
    private static final String STATUS = "status";
    private static final String NAME = "name";
    private static final String ADDRESS_PAIR = "allowed_address_pairs";
    private static final String ADMIN_STATUS = "admin_status";
    private static final String NETWORK_ID = "network_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String DEVICE_OWNER = "device_owner";
    private static final String MAC_ADDRESS = "mac_address";
    private static final String FIXED_IPS = "fixed_ips";
    private static final String SUBNET_ID = "subnet_id";
    private static final String IP_ADDRESS = "ip_address";
    private static final String ID = "id";
    private static final String SECURITY_GROUPS = "security_groups";
    private static final String DEVICE_ID = "device_id";

    @Override
    public OpenstackPort decode(ObjectNode json, CodecContext context) {

        HashMap<String, Ip4Address> fixedIpMap = new HashMap<>();
        JsonNode portInfo = json.get(PORT);
        if (portInfo == null) {
            portInfo = json;
        }

        String status = portInfo.path(STATUS).asText();
        String name = portInfo.path(NAME).asText();
        boolean adminStateUp = portInfo.path(ADMIN_STATUS).asBoolean();
        String networkId = portInfo.path(NETWORK_ID).asText();
        String tenantId = portInfo.path(TENANT_ID).asText();
        String deviceOwner = portInfo.path(DEVICE_OWNER).asText();
        String macStr = portInfo.path(MAC_ADDRESS).asText();
        ArrayNode fixedIpList = (ArrayNode) portInfo.path(FIXED_IPS);
        for (JsonNode fixedIpInfo: fixedIpList) {
            String subnetId = fixedIpInfo.path(SUBNET_ID).asText();
            String ipAddressStr = fixedIpInfo.path(IP_ADDRESS).asText();
            if (ipAddressStr != null) {
                Ip4Address ipAddress = Ip4Address.valueOf(ipAddressStr);
                fixedIpMap.put(subnetId, ipAddress);
            }
        }
        String id = portInfo.path(ID).asText();
        String securityGroups = portInfo.path(SECURITY_GROUPS).asText();
        String deviceId = portInfo.path(DEVICE_ID).asText();

        OpenstackPort.Builder openstackPortBuilder = OpenstackPort.builder();
        OpenstackPort.PortStatus portStatus =
                status.equals("N/A") ? OpenstackPort.PortStatus.NA :
                        OpenstackPort.PortStatus.valueOf(status);

        openstackPortBuilder.portStatus(portStatus)
                .name(name)
                .adminState(adminStateUp)
                .netwrokId(networkId)
                .tenantId(tenantId)
                .deviceOwner(deviceOwner)
                .macAddress(MacAddress.valueOf(macStr))
                .fixedIps(fixedIpMap)
                .id(id)
                .deviceId(deviceId);

        // FIX ME
        if (!securityGroups.isEmpty()) {
            openstackPortBuilder.securityGroup(securityGroups);
        }

        OpenstackPort openstackPort = openstackPortBuilder.build();

        return openstackPort;
    }

}
