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
package org.onosproject.incubator.net.virtual.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.virtual.DefaultVirtualHost;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the VirtualHost class.
 */
public class VirtualHostCodec extends JsonCodec<VirtualHost> {

    // JSON field names
    static final String NETWORK_ID = "networkId";
    static final String HOST_ID = "id";
    static final String MAC_ADDRESS = "mac";
    static final String VLAN = "vlan";
    static final String IP_ADDRESSES = "ipAddresses";
    static final String HOST_LOCATION = "locations";

    private static final String NULL_OBJECT_MSG = "VirtualHost cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in VirtualHost";

    @Override
    public ObjectNode encode(VirtualHost vHost, CodecContext context) {
        checkNotNull(vHost, NULL_OBJECT_MSG);

        final JsonCodec<HostLocation> locationCodec =
                context.codec(HostLocation.class);
        final ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK_ID, vHost.networkId().toString())
                .put(HOST_ID, vHost.id().toString())
                .put(MAC_ADDRESS, vHost.mac().toString())
                .put(VLAN, vHost.vlan().toString());

        final ArrayNode jsonIpAddresses = result.putArray(IP_ADDRESSES);
        for (final IpAddress ipAddress : vHost.ipAddresses()) {
            jsonIpAddresses.add(ipAddress.toString());
        }
        result.set(IP_ADDRESSES, jsonIpAddresses);

        final ArrayNode jsonLocations = result.putArray("locations");
        for (final HostLocation location : vHost.locations()) {
            jsonLocations.add(locationCodec.encode(location, context));
        }
        result.set("locations", jsonLocations);

        return result;
    }

    @Override
    public VirtualHost decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        NetworkId nId = NetworkId.networkId(Long.parseLong(extractMember(NETWORK_ID, json)));
        MacAddress mac = MacAddress.valueOf(json.get("mac").asText());
        VlanId vlanId = VlanId.vlanId((short) json.get("vlan").asInt(VlanId.UNTAGGED));

        Set<HostLocation> locations = new HashSet<>();
        JsonNode locationNodes = json.get("locations");
        locationNodes.forEach(locationNode -> {
            PortNumber portNumber = PortNumber.portNumber(locationNode.get("port").asText());
            DeviceId deviceId = DeviceId.deviceId(locationNode.get("elementId").asText());
            locations.add(new HostLocation(deviceId, portNumber, 0));
        });

        HostId id = HostId.hostId(mac, vlanId);

        Iterator<JsonNode> ipStrings = json.get("ipAddresses").elements();
        Set<IpAddress> ips = new HashSet<>();
        while (ipStrings.hasNext()) {
            ips.add(IpAddress.valueOf(ipStrings.next().asText()));
        }

        return new DefaultVirtualHost(nId, id, mac, vlanId, locations, ips);
    }

    /**
     * Extract member from JSON ObjectNode.
     *
     * @param key key for which value is needed
     * @param json JSON ObjectNode
     * @return member value
     */
    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
