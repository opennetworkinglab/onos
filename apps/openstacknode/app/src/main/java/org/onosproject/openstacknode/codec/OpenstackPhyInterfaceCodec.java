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
package org.onosproject.openstacknode.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacknode.api.DefaultOpenstackPhyInterface;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Openstack physical interface codec used for serializing and de-serializing JSON string.
 */
public final class OpenstackPhyInterfaceCodec extends JsonCodec<OpenstackPhyInterface> {

    private static final String NETWORK = "network";
    private static final String INTERFACE = "intf";

    private static final String MISSING_MESSAGE = " is required in OpenstackPhyInterface";

    @Override
    public ObjectNode encode(OpenstackPhyInterface phyIntf, CodecContext context) {
        checkNotNull(phyIntf, "Openstack physical interface cannot be null");

        return context.mapper().createObjectNode()
                .put(NETWORK, phyIntf.network())
                .put(INTERFACE, phyIntf.intf());
    }

    @Override
    public OpenstackPhyInterface decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String network = nullIsIllegal(json.get(NETWORK).asText(),
                NETWORK + MISSING_MESSAGE);
        String intf = nullIsIllegal(json.get(INTERFACE).asText(),
                INTERFACE + MISSING_MESSAGE);

        return DefaultOpenstackPhyInterface.builder()
                        .network(network)
                        .intf(intf)
                        .build();
    }
}
