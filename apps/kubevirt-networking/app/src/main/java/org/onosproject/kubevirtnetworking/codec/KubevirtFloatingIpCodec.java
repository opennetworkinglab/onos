/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt floating IP codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtFloatingIpCodec extends JsonCodec<KubevirtFloatingIp> {

    private final Logger log = getLogger(getClass());

    private static final String ID = "id";
    private static final String ROUTER_NAME = "routerName";
    private static final String POD_NAME = "podName";
    private static final String VM_NAME = "vmName";
    private static final String NETWORK_NAME = "networkName";
    private static final String FLOATING_IP = "floatingIp";
    private static final String FIXED_IP = "fixedIp";

    private static final String MISSING_MESSAGE = " is required in KubevirtFloatingIp";

    @Override
    public ObjectNode encode(KubevirtFloatingIp fip, CodecContext context) {
        checkNotNull(fip, "Kubevirt floating IP cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, fip.id())
                .put(ROUTER_NAME, fip.routerName())
                .put(NETWORK_NAME, fip.networkName())
                .put(FLOATING_IP, fip.floatingIp().toString());

        if (fip.podName() != null) {
            result.put(POD_NAME, fip.podName());
        }

        if (fip.vmName() != null) {
            result.put(VM_NAME, fip.vmName());
        }

        if (fip.fixedIp() != null) {
            result.put(FIXED_IP, fip.fixedIp().toString());
        }

        return result;
    }

    @Override
    public KubevirtFloatingIp decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String id = nullIsIllegal(json.get(ID).asText(), ID + MISSING_MESSAGE);
        String routerName = nullIsIllegal(json.get(ROUTER_NAME).asText(),
                ROUTER_NAME + MISSING_MESSAGE);
        String floatingIp = nullIsIllegal(json.get(FLOATING_IP).asText(),
                FLOATING_IP + MISSING_MESSAGE);
        String networkName = nullIsIllegal(json.get(NETWORK_NAME).asText(),
                NETWORK_NAME + MISSING_MESSAGE);

        KubevirtFloatingIp.Builder builder = DefaultKubevirtFloatingIp.builder()
                .id(id)
                .routerName(routerName)
                .networkName(networkName)
                .floatingIp(IpAddress.valueOf(floatingIp));

        JsonNode podName = json.get(POD_NAME);
        if (podName != null) {
            builder.podName(podName.asText());
        }

        JsonNode vmName = json.get(VM_NAME);
        if (vmName != null) {
            builder.vmName(vmName.asText());
        }

        JsonNode fixedIp = json.get(FIXED_IP);
        if (fixedIp != null) {
            builder.fixedIp(IpAddress.valueOf(fixedIp.asText()));
        }

        return builder.build();
    }
}
