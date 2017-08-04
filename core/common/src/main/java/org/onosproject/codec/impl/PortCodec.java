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

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onlab.packet.ChassisId;
import org.onosproject.codec.CodecContext;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Device port JSON codec.
 */
public final class PortCodec extends AnnotatedCodec<Port> {

    // JSON field names
    private static final String ELEMENT = "element"; // DeviceId
    private static final String PORT_NAME = "port";
    private static final String IS_ENABLED = "isEnabled";
    private static final String TYPE = "type";
    private static final String PORT_SPEED = "portSpeed";

    // Special port name alias
    private static final String PORT_NAME_LOCAL = "local";

    @Override
    public ObjectNode encode(Port port, CodecContext context) {
        checkNotNull(port, "Port cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ELEMENT, port.element().id().toString())
                .put(PORT_NAME, portName(port.number()))
                .put(IS_ENABLED, port.isEnabled())
                .put(TYPE, port.type().toString().toLowerCase())
                .put(PORT_SPEED, port.portSpeed());
        return annotate(result, port, context);
    }

    private String portName(PortNumber port) {
        return port.equals(PortNumber.LOCAL) ? PORT_NAME_LOCAL : port.toString();
    }

    private static PortNumber portNumber(String portName) {
        if (portName.equalsIgnoreCase(PORT_NAME_LOCAL)) {
            return PortNumber.LOCAL;
        }

        return PortNumber.portNumber(portName);
    }


    /**
     * {@inheritDoc}
     *
     * Note: Result of {@link Port#element()} returned Port object,
     *       is not a full Device object.
     *       Only it's DeviceId can be used.
     */
    @Override
    public Port decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        DeviceId did = DeviceId.deviceId(json.get(ELEMENT).asText());
        Device device = new DummyDevice(did);
        PortNumber number = portNumber(json.get(PORT_NAME).asText());
        boolean isEnabled = json.get(IS_ENABLED).asBoolean();
        Type type = Type.valueOf(json.get(TYPE).asText().toUpperCase());
        long portSpeed = json.get(PORT_SPEED).asLong();
        Annotations annotations = extractAnnotations(json, context);

        return new DefaultPort(device, number, isEnabled, type, portSpeed, annotations);
    }


    /**
     * Dummy Device which only holds DeviceId.
     */
    private static final class DummyDevice extends DefaultDevice {
        /**
         * Constructs Dummy Device which only holds DeviceId.
         *
         * @param did device Id
         */
        public DummyDevice(DeviceId did) {
            super(new ProviderId(did.uri().getScheme(), "PortCodec"), did,
                  Type.SWITCH, "dummy", "0", "0", "0", new ChassisId(),
                  DefaultAnnotations.EMPTY);
        }
    }
}
