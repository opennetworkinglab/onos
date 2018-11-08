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
package org.onosproject.openstackvtap.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapNetwork;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Openstack vtap network codec used for serializing and de-serializing JSON string.
 */
public final class OpenstackVtapNetworkCodec extends JsonCodec<OpenstackVtapNetwork> {

    private final Logger log = getLogger(getClass());

    private static final String MODE = "mode";
    private static final String NETWORK_ID = "networkId";
    private static final String SERVER_IP = "serverIp";

    private static final String JSON_NULL_MESSAGE = "% cannot be null";
    private static final String JSON_MISSING_MESSAGE = "% is required";
    private static final String JSON_TYPE_MESSAGE = "% is not json object type";

    @Override
    public ObjectNode encode(OpenstackVtapNetwork network, CodecContext context) {
        checkNotNull(network, JSON_NULL_MESSAGE, "OpenstackVtapNetwork object");

        ObjectNode result = context.mapper().createObjectNode()
                .put(MODE, network.mode().toString())
                .put(NETWORK_ID, network.networkId())
                .put(SERVER_IP, network.serverIp().toString());

        return result;
    }

    @Override
    public OpenstackVtapNetwork decode(ObjectNode json, CodecContext context) {
        checkNotNull(json, JSON_NULL_MESSAGE, "OpenstackVtapNetwork json");
        checkState(json.isObject(), JSON_TYPE_MESSAGE, "OpenstackVtapNetwork json");

        DefaultOpenstackVtapNetwork.Builder builder = DefaultOpenstackVtapNetwork.builder()
                .mode(OpenstackVtapNetwork.Mode.valueOf(checkNotNull(json.get(MODE).asText(null),
                        JSON_MISSING_MESSAGE, MODE)))
                .networkId(json.get(NETWORK_ID).asInt(0))
                .serverIp(IpAddress.valueOf(checkNotNull(json.get(SERVER_IP).asText(null),
                        JSON_MISSING_MESSAGE, SERVER_IP)));

        log.debug("OpenstackVtapNetwork is {}", builder.build().toString());
        return builder.build();
    }
}
