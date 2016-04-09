/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Connection point JSON codec.
 */
public final class ConnectPointCodec extends JsonCodec<ConnectPoint> {

    // JSON field names
    private static final String ELEMENT_HOST = "host";
    private static final String ELEMENT_DEVICE = "device";
    private static final String PORT = "port";

    @Override
    public ObjectNode encode(ConnectPoint point, CodecContext context) {
        checkNotNull(point, "Connect point cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(PORT, point.port().toString());

        if (point.elementId() instanceof DeviceId) {
            root.put(ELEMENT_DEVICE, point.deviceId().toString());
        } else if (point.elementId() instanceof HostId) {
            root.put(ELEMENT_HOST, point.hostId().toString());
        }

        return root;
    }

    @Override
    public ConnectPoint decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        ElementId elementId;
        if (json.has(ELEMENT_DEVICE)) {
            elementId = DeviceId.deviceId(json.get(ELEMENT_DEVICE).asText());
        } else if (json.has(ELEMENT_HOST)) {
            elementId = HostId.hostId(json.get(ELEMENT_HOST).asText());
        } else {
            // invalid JSON
            return null;
        }
        PortNumber portNumber = portNumber(json.get(PORT).asText());
        return new ConnectPoint(elementId, portNumber);
    }
}
