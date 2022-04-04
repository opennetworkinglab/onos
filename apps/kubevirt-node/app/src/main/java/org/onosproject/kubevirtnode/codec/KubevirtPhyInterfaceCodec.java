/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnode.api.DefaultKubevirtPhyInterface;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Kubevirt physical interface codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtPhyInterfaceCodec extends JsonCodec<KubevirtPhyInterface> {

    private static final String NETWORK = "network";
    private static final String INTERFACE = "intf";
    private static final String PHYS_BRIDGE_ID = "physBridgeId";

    private static final String MISSING_MESSAGE = " is required in KubevirtPhyInterface";

    @Override
    public ObjectNode encode(KubevirtPhyInterface phyIntf, CodecContext context) {
        checkNotNull(phyIntf, "Kubevirt physical interface cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK, phyIntf.network())
                .put(INTERFACE, phyIntf.intf());

        if (phyIntf.physBridge() != null) {
            result.put(PHYS_BRIDGE_ID, phyIntf.physBridge().toString());
        }

        return result;
    }

    @Override
    public KubevirtPhyInterface decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String network = nullIsIllegal(json.get(NETWORK).asText(),
                NETWORK + MISSING_MESSAGE);
        String intf = nullIsIllegal(json.get(INTERFACE).asText(),
                INTERFACE + MISSING_MESSAGE);

        KubevirtPhyInterface.Builder builder = DefaultKubevirtPhyInterface.builder()
                .network(network)
                .intf(intf);

        JsonNode physBridgeJson = json.get(PHYS_BRIDGE_ID);
        if (physBridgeJson != null) {
            builder.physBridge(DeviceId.deviceId(physBridgeJson.asText()));
        }

        return builder.build();
    }
}
