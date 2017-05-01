/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.impl.DefaultOFController;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * OFController JSON codec.
 */
public final class OFControllerCodec extends JsonCodec<OFController> {

    private static final String IP = "ip";
    private static final String PORT = "port";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in OFController";

    @Override
    public ObjectNode encode(OFController ofController, CodecContext context) {
        checkNotNull(ofController, "OFController cannot be null");

        return context.mapper().createObjectNode()
                .put(IP, String.valueOf(ofController.ip()))
                .put(PORT, String.valueOf(ofController.port()));

    }

    @Override
    public OFController decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse ip address
        int id = nullIsIllegal(json.get(IP), IP + MISSING_MEMBER_MESSAGE).asInt();

        // parse port
        String name = nullIsIllegal(json.get(PORT), PORT + MISSING_MEMBER_MESSAGE).asText();

        return DefaultOFController.of(IpAddress.valueOf(id),
                                      TpPort.tpPort(Integer.valueOf(name)));
    }
}
