/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostLocation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * HostLocation JSON codec.
 */
public final class HostLocationCodec extends JsonCodec<HostLocation> {

    public static final String ELEMENT_ID = "elementId";
    public static final String PORT = "port";

    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in HostLocation";

    @Override
    public ObjectNode encode(HostLocation hostLocation, CodecContext context) {
        checkNotNull(hostLocation, "Host location cannot be null");
        return context.mapper().createObjectNode()
                .put(ELEMENT_ID, hostLocation.elementId().toString())
                .put(PORT, hostLocation.port().toString());
    }

    @Override
    public HostLocation decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        DeviceId deviceId = DeviceId.deviceId(nullIsIllegal(
                json.get(ELEMENT_ID), ELEMENT_ID + MISSING_MEMBER_MESSAGE).asText());
        PortNumber portNumber = PortNumber.portNumber(nullIsIllegal(
                json.get(PORT), PORT + MISSING_MEMBER_MESSAGE).asText());

        return new HostLocation(deviceId, portNumber, 0);
    }
}
