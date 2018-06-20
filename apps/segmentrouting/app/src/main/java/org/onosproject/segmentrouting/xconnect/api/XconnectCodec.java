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
package org.onosproject.segmentrouting.xconnect.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class XconnectCodec extends JsonCodec<XconnectDesc> {
    private static final String DEVICE_ID = "deviceId";
    private static final String VLAN_ID = "vlanId";
    private static final String PORTS = "ports";

    private static Logger log = LoggerFactory.getLogger(XconnectCodec.class);

    @Override
    public ObjectNode encode(XconnectDesc desc, CodecContext context) {
        final ObjectNode result = context.mapper().createObjectNode();
        result.put(DEVICE_ID, desc.key().deviceId().toString());
        result.put(VLAN_ID, desc.key().vlanId().toString());
        final ArrayNode portNode = result.putArray(PORTS);
        desc.ports().forEach(port -> portNode.add(port.toString()));

        return result;
    }

    @Override
    public XconnectDesc decode(ObjectNode json, CodecContext context) {
        DeviceId deviceId = DeviceId.deviceId(json.path(DEVICE_ID).asText());
        VlanId vlanId = VlanId.vlanId(json.path(VLAN_ID).asText());

        Set<PortNumber> ports = Sets.newHashSet();
        JsonNode portNodes = json.get(PORTS);
        if (portNodes != null) {
            portNodes.forEach(portNode -> ports.add(PortNumber.portNumber(portNode.asInt())));
        }

        XconnectKey key = new XconnectKey(deviceId, vlanId);
        return new XconnectDesc(key, ports);
    }
}
