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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip4Address;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the OpenstackFloatingIP Codec.
 */
public class OpenstackFloatingIpCodec extends JsonCodec<OpenstackFloatingIP> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String FLOATINGIP = "floatingip";
    private static final String FLOATING_NETWORK_ID = "floating_network_id";
    private static final String ROUTER_ID = "router_id";
    private static final String FIXED_IP_ADDRESS = "fixed_ip_address";
    private static final String FLOATING_IP_ADDRESS = "floating_ip_address";
    private static final String TENANT_ID = "tenant_id";
    private static final String STATUS = "status";
    private static final String PORT_ID = "port_id";
    private static final String ID = "id";

    /**
     * Decodes the OpenstackFloatingIP.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return OpenstackFloatingIP
     */
    @Override
    public OpenstackFloatingIP decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode floatingIpInfo = json.get(FLOATINGIP);
        if (floatingIpInfo == null) {
            floatingIpInfo = json;
        }

        String networkId = floatingIpInfo.path(FLOATING_NETWORK_ID).asText();
        String routerId = floatingIpInfo.path(ROUTER_ID).asText();
        String fixedIpAddressStr = floatingIpInfo.path(FIXED_IP_ADDRESS).asText();
        String floatingIpAddressStr = floatingIpInfo.path(FLOATING_IP_ADDRESS).asText();
        String tenantId = floatingIpInfo.path(TENANT_ID).asText();
        String statusStr = floatingIpInfo.path(STATUS).asText();
        String portId = floatingIpInfo.path(PORT_ID).asText();
        String id = floatingIpInfo.path(ID).asText();

        checkNotNull(networkId);
        checkNotNull(floatingIpAddressStr);
        checkNotNull(tenantId);
        checkNotNull(statusStr);
        checkNotNull(id);

        if (routerId != null && routerId.equals("null")) {
            routerId = null;
        }

        Ip4Address fixedIpAddress = null;
        if (fixedIpAddressStr != null && !fixedIpAddressStr.equals("null")) {
            fixedIpAddress = Ip4Address.valueOf(fixedIpAddressStr);
        }

        Ip4Address floatingIpAddress = Ip4Address.valueOf(floatingIpAddressStr);

        OpenstackFloatingIP.FloatingIpStatus status =
                OpenstackFloatingIP.FloatingIpStatus.valueOf(statusStr);

        if (portId != null && portId.equals("null")) {
            portId = null;
        }

        OpenstackFloatingIP.Builder osFloatingIpBuilder =
                new OpenstackFloatingIP.Builder();

        return osFloatingIpBuilder.networkId(networkId)
                .routerId(routerId)
                .fixedIpAddress(fixedIpAddress)
                .floatingIpAddress(floatingIpAddress)
                .tenantId(tenantId)
                .status(status)
                .portId(portId)
                .id(id)
                .build();
    }

}
