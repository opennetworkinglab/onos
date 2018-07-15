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
package org.onosproject.openstacknetworking.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.impl.DefaultInstancePort;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Openstack instance port codec used for serializing and de-serializing JSON string.
 */
public class InstancePortCodec extends JsonCodec<InstancePort> {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_ID = "networkId";
    private static final String PORT_ID = "portId";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String STATE = "state";

    private static final String MISSING_MESSAGE = " is required in InstancePort";

    @Override
    public ObjectNode encode(InstancePort port, CodecContext context) {
        checkNotNull(port, "Instance port cannot be null");

        return context.mapper().createObjectNode()
                .put(NETWORK_ID, port.networkId())
                .put(PORT_ID, port.portId())
                .put(MAC_ADDRESS, port.macAddress().toString())
                .put(IP_ADDRESS, port.ipAddress().toString())
                .put(DEVICE_ID, port.deviceId().toString())
                .put(PORT_NUMBER, port.portNumber().toString())
                .put(STATE, port.state().name());
    }

    @Override
    public InstancePort decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String networkId = nullIsIllegal(json.get(NETWORK_ID).asText(),
                NETWORK_ID + MISSING_MESSAGE);
        String portId = nullIsIllegal(json.get(PORT_ID).asText(),
                PORT_ID + MISSING_MESSAGE);
        String macAddress = nullIsIllegal(json.get(MAC_ADDRESS).asText(),
                MAC_ADDRESS + MISSING_MESSAGE);
        String ipAddress = nullIsIllegal(json.get(IP_ADDRESS).asText(),
                IP_ADDRESS + MISSING_MESSAGE);
        String deviceId = nullIsIllegal(json.get(DEVICE_ID).asText(),
                DEVICE_ID + MISSING_MESSAGE);
        String portNumber = nullIsIllegal(json.get(PORT_NUMBER).asText(),
                PORT_NUMBER + MISSING_MESSAGE);
        String state = nullIsIllegal(json.get(STATE).asText(),
                STATE + MISSING_MESSAGE);

        return DefaultInstancePort.builder()
                .networkId(networkId)
                .portId(portId)
                .macAddress(MacAddress.valueOf(macAddress))
                .ipAddress(IpAddress.valueOf(ipAddress))
                .deviceId(DeviceId.deviceId(deviceId))
                .portNumber(PortNumber.fromString(portNumber))
                .state(InstancePort.State.valueOf(state)).build();
    }
}
