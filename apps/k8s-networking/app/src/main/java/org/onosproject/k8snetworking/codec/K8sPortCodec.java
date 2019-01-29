/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.k8snetworking.api.DefaultK8sPort;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snetworking.api.K8sPort.State;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes port codec used for serializing and de-serializing JSON string.
 */
public final class K8sPortCodec extends JsonCodec<K8sPort> {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_ID = "networkId";
    private static final String PORT_ID = "portId";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String STATE = "state";

    private static final String MISSING_MESSAGE = " is required in K8sPort";

    @Override
    public ObjectNode encode(K8sPort port, CodecContext context) {
        checkNotNull(port, "Kubernetes port cannot be null");

        ObjectNode result =  context.mapper().createObjectNode()
                .put(NETWORK_ID, port.networkId())
                .put(PORT_ID, port.portId())
                .put(MAC_ADDRESS, port.macAddress().toString())
                .put(IP_ADDRESS, port.ipAddress().toString());

        if (port.deviceId() != null) {
            result.put(DEVICE_ID, port.deviceId().toString());
        }

        if (port.portNumber() != null) {
            result.put(PORT_NUMBER, port.portNumber().toString());
        }

        if (port.state() != null) {
            result.put(STATE, port.state().name());
        }

        return result;
    }

    @Override
    public K8sPort decode(ObjectNode json, CodecContext context) {
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

        K8sPort.Builder builder = DefaultK8sPort.builder()
                .networkId(networkId)
                .portId(portId)
                .macAddress(MacAddress.valueOf(macAddress))
                .ipAddress(IpAddress.valueOf(ipAddress));

        JsonNode deviceIdJson = json.get(DEVICE_ID);
        if (deviceIdJson != null) {
            builder.deviceId(DeviceId.deviceId(deviceIdJson.asText()));
        }

        JsonNode portNumberJson = json.get(PORT_NUMBER);
        if (portNumberJson != null) {
            builder.portNumber(PortNumber.portNumber(portNumberJson.asText()));
        }

        JsonNode stateJson = json.get(STATE);
        if (stateJson != null) {
            builder.state(State.valueOf(stateJson.asText()));
        } else {
            builder.state(State.INACTIVE);
        }

        return builder.build();
    }
}
