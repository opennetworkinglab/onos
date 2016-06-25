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
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackinterface.OpenstackNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the OpenstackNetwork Codec.
 *
 */
public class OpenstackNetworkCodec extends JsonCodec<OpenstackNetwork> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NETWORK = "network";
    private static final String NAME = "name";
    private static final String TENANT_ID = "tenant_id";
    private static final String SEGMENTATION_ID = "provider:segmentation_id";
    private static final String NETWORK_TYPE = "provider:network_type";
    private static final String ID = "id";

    @Override
    public OpenstackNetwork decode(ObjectNode json, CodecContext context) {

        JsonNode networkInfo = json.get(NETWORK);
        if (networkInfo == null) {
            networkInfo = json;
        }

        String name = networkInfo.path(NAME).asText();
        String tenantId = networkInfo.path(TENANT_ID).asText();
        String id = networkInfo.path(ID).asText();

        OpenstackNetwork.Builder onb = OpenstackNetwork.builder();
        onb.name(name)
                .tenantId(tenantId)
                .id(id);

        if (networkInfo.path(NETWORK_TYPE).isMissingNode()) {
            log.debug("Network {} has no network type, ignore it.", name);
            return null;
        }

        String networkType = networkInfo.path(NETWORK_TYPE).asText();
        try {
            onb.networkType(OpenstackNetwork.NetworkType.valueOf(networkType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.debug("Network {} has unsupported network type {}, ignore it.",
                     name, networkType);
            return null;
        }

        onb.segmentId(networkInfo.path(SEGMENTATION_ID).asText());

        return onb.build();
    }
}
