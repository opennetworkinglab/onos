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
package org.onosproject.openstacknetworking.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.impl.DefaultExternalPeerRouter;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Openstack external peer router codec used for serializing and de-serializing JSON string.
 */
public class ExternalPeerRouterCodec extends JsonCodec<ExternalPeerRouter> {

    private final Logger log = getLogger(getClass());

    private static final String IP_ADDRESS = "ipAddress";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String VLAN_ID = "vlanId";

    private static final String MISSING_MESSAGE = " is required in ExternalPeerRouter";

    @Override
    public ObjectNode encode(ExternalPeerRouter router, CodecContext context) {
        checkNotNull(router, "External peer router cannot be null");

        return context.mapper().createObjectNode()
                .put(IP_ADDRESS, router.ipAddress().toString())
                .put(MAC_ADDRESS, router.macAddress().toString())
                .put(VLAN_ID, router.vlanId().toString());
    }

    @Override
    public ExternalPeerRouter decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String ipAddress = nullIsIllegal(json.get(IP_ADDRESS).asText(),
                IP_ADDRESS + MISSING_MESSAGE);
        String macAddress = nullIsIllegal(json.get(MAC_ADDRESS).asText(),
                MAC_ADDRESS + MISSING_MESSAGE);
        String vlanId = nullIsIllegal(json.get(VLAN_ID).asText(),
                VLAN_ID + MISSING_MESSAGE);

        return DefaultExternalPeerRouter.builder()
                .ipAddress(IpAddress.valueOf(ipAddress))
                .macAddress(MacAddress.valueOf(macAddress))
                .vlanId(VlanId.vlanId(vlanId)).build();
    }
}
