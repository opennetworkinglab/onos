/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

public final class KubevirtPortCodec extends JsonCodec<KubevirtPort> {

    private final Logger log = getLogger(getClass());

    private static final String VM_NAME = "vmName";
    private static final String NETWORK_ID = "networkId";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String SECURITY_GROUPS = "securityGroups";

    private static final String MISSING_MESSAGE = " is required in KubevirtPort";

    @Override
    public ObjectNode encode(KubevirtPort port, CodecContext context) {
        checkNotNull(port, "Kubevirt port cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(VM_NAME, port.vmName())
                .put(NETWORK_ID, port.networkId())
                .put(MAC_ADDRESS, port.macAddress().toString());

        if (port.ipAddress() != null) {
            result.put(IP_ADDRESS, port.ipAddress().toString());
        }

        if (port.deviceId() != null) {
            result.put(DEVICE_ID, port.deviceId().toString());
        }

        if (port.portNumber() != null) {
            result.put(PORT_NUMBER, port.portNumber().toString());
        }

        if (port.securityGroups() != null) {
            ArrayNode sgIds = context.mapper().createArrayNode();
            for (String sgId : port.securityGroups()) {
                sgIds.add(sgId);
            }
            result.set(SECURITY_GROUPS, sgIds);
        }

        return result;
    }

    @Override
    public KubevirtPort decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String vmName = nullIsIllegal(json.get(VM_NAME).asText(),
                VM_NAME + MISSING_MESSAGE);

        String networkId = nullIsIllegal(json.get(NETWORK_ID).asText(),
                NETWORK_ID + MISSING_MESSAGE);

        String macAddress = nullIsIllegal(json.get(MAC_ADDRESS).asText(),
                MAC_ADDRESS + MISSING_MESSAGE);

        KubevirtPort.Builder builder = DefaultKubevirtPort.builder()
                .vmName(vmName)
                .networkId(networkId)
                .macAddress(MacAddress.valueOf(macAddress));

        JsonNode ipAddressJson = json.get(IP_ADDRESS);
        if (ipAddressJson != null) {
            final IpAddress ipAddress = IpAddress.valueOf(ipAddressJson.asText());
            builder.ipAddress(ipAddress);
        }

        JsonNode deviceIdJson = json.get(DEVICE_ID);
        if (deviceIdJson != null) {
            final DeviceId deviceId = DeviceId.deviceId(deviceIdJson.asText());
            builder.deviceId(deviceId);
        }

        JsonNode portNumberJson = json.get(PORT_NUMBER);
        if (portNumberJson != null) {
            final PortNumber portNumber = PortNumber.portNumber(portNumberJson.asText());
            builder.portNumber(portNumber);
        }

        log.trace("Port is {}", builder.build().toString());

        return builder.build();
    }
}
