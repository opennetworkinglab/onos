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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.virtual.DefaultVirtualPort;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the VirtualPort class.
 */
public class VirtualPortCodec extends JsonCodec<VirtualPort> {

    // JSON field names
    private static final String NETWORK_ID = "networkId";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUM = "portNum";
    private static final String PHYS_DEVICE_ID = "physDeviceId";
    private static final String PHYS_PORT_NUM = "physPortNum";

    private static final String NULL_OBJECT_MSG = "VirtualPort cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in VirtualPort";
    private static final String INVALID_VIRTUAL_DEVICE = " is not a valid VirtualDevice";

    @Override
    public ObjectNode encode(VirtualPort vPort, CodecContext context) {
        checkNotNull(vPort, NULL_OBJECT_MSG);

        ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK_ID, vPort.networkId().toString())
                .put(DEVICE_ID, vPort.element().id().toString())
                .put(PORT_NUM, vPort.number().toString())
                .put(PHYS_DEVICE_ID, vPort.realizedBy().deviceId().toString())
                .put(PHYS_PORT_NUM, vPort.realizedBy().port().toString());

        return result;
    }

    @Override
    public VirtualPort decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        NetworkId nId = NetworkId.networkId(Long.parseLong(extractMember(NETWORK_ID, json)));
        DeviceId dId = DeviceId.deviceId(extractMember(DEVICE_ID, json));

        VirtualNetworkService vnetService = context.getService(VirtualNetworkService.class);
        Set<VirtualDevice> vDevs = vnetService.getVirtualDevices(nId);
        VirtualDevice vDev = vDevs.stream()
                .filter(virtualDevice -> virtualDevice.id().equals(dId))
                .findFirst().orElse(null);
        nullIsIllegal(vDev, dId.toString() + INVALID_VIRTUAL_DEVICE);

        PortNumber portNum = PortNumber.portNumber(extractMember(PORT_NUM, json));
        DeviceId physDId = DeviceId.deviceId(extractMember(PHYS_DEVICE_ID, json));
        PortNumber physPortNum = PortNumber.portNumber(extractMember(PHYS_PORT_NUM, json));

        ConnectPoint realizedBy = new ConnectPoint(physDId, physPortNum);
        return new DefaultVirtualPort(nId, vDev, portNum, realizedBy);
    }

    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
