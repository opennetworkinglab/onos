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
import com.google.common.collect.Maps;
import org.onlab.packet.Ip4Address;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackinterface.OpenstackExternalGateway;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
/**
 * Implementation of the OpenstackRouter Codec.
 */
public class OpenstackRouterCodec extends JsonCodec<OpenstackRouter> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ROUTER = "router";
    private static final String TENANT_ID = "tenant_id";
    private static final String NETWORK_ID = "network_id";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String STATUS = "status";
    private static final String ADMIN_STATE_UP = "admin_state_up";
    private static final String EXTERNAL_GW_INFO = "external_gateway_info";
    private static final String EXTERNAL_FIXED_IPS = "external_fixed_ips";
    private static final String SUBNET_ID = "subnet_id";
    private static final String IP_ADDRESS = "ip_address";

    /**
     * Decodes the OpenstackRouter.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return OpenstackRouter
     */
    @Override
    public OpenstackRouter decode(ObjectNode json, CodecContext context) {

        if (json == null || !json.isObject()) {
            return null;
        }
        JsonNode routerInfo = json.get(ROUTER);
        if (routerInfo == null) {
            routerInfo = json;
        }

        String tenantId = checkNotNull(routerInfo.path(TENANT_ID).asText());
        String id = checkNotNull(routerInfo.path(ID).asText());
        String name = checkNotNull(routerInfo.path(NAME).asText());
        String adminStateUp = checkNotNull(routerInfo.path(ADMIN_STATE_UP).asText());

        OpenstackExternalGateway.Builder osExtBuiler = new OpenstackExternalGateway.Builder();

        if (!routerInfo.path(EXTERNAL_GW_INFO).isMissingNode()) {
            String externalGatewayNetId = checkNotNull(routerInfo.path(EXTERNAL_GW_INFO).path(NETWORK_ID).asText());
            Map<String, Ip4Address> fixedIpMap = Maps.newHashMap();


            if (!routerInfo.path(EXTERNAL_GW_INFO).path(EXTERNAL_FIXED_IPS).isMissingNode()) {
                ArrayNode fixedIpList = (ArrayNode) routerInfo.path(EXTERNAL_GW_INFO).path(EXTERNAL_FIXED_IPS);

                for (JsonNode fixedIpInfo : fixedIpList) {
                    String subnetId = checkNotNull(fixedIpInfo.path(SUBNET_ID).asText());
                    String ipAddressStr = checkNotNull(fixedIpInfo.path(IP_ADDRESS).asText());
                    if (!fixedIpInfo.path(IP_ADDRESS).isMissingNode() && ipAddressStr != null) {
                        fixedIpMap.put(subnetId, Ip4Address.valueOf(ipAddressStr));
                    }
                }
            }

            osExtBuiler.networkId(externalGatewayNetId)
                    .enablePnat(true)
                    .externalFixedIps(fixedIpMap);
        }
        OpenstackRouter.Builder osBuilder = new OpenstackRouter.Builder()
                .tenantId(tenantId)
                .id(id)
                .name(name)
                .status(OpenstackRouter.RouterStatus.ACTIVE)
                .adminStateUp(Boolean.valueOf(adminStateUp))
                .gatewayExternalInfo(osExtBuiler.build());

        return osBuilder.build();
    }
}
