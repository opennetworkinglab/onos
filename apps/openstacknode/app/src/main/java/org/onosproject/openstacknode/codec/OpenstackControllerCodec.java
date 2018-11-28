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
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.behaviour.ControllerInfo;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Openstack controller codec used for serializing and de-serializing JSON string.
 */
public class OpenstackControllerCodec extends JsonCodec<ControllerInfo> {

    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String TCP = "tcp";  // controller connection should always be TCP

    private static final String MISSING_MESSAGE = " is required in ControllerInfo";

    @Override
    public ObjectNode encode(ControllerInfo controller, CodecContext context) {
        checkNotNull(controller, "Openstack controller cannot be null");

        return context.mapper().createObjectNode()
                .put(IP, controller.ip().toString())
                .put(PORT, controller.port());
    }

    @Override
    public ControllerInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String ip = nullIsIllegal(json.get(IP).asText(),
                IP + MISSING_MESSAGE);
        int port = nullIsIllegal(json.get(PORT).asInt(),
                PORT + MISSING_MESSAGE);

        return new ControllerInfo(IpAddress.valueOf(ip), port, TCP);
    }
}
