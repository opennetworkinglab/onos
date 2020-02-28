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

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.provider.ProviderId;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON codec for Host class.
 */
public final class HostCodec extends AnnotatedCodec<Host> {

    // JSON field names
    public static final String HOST_ID = "id";
    public static final String MAC = "mac";
    public static final String VLAN = "vlan";
    public static final String IS_CONFIGURED = "configured";
    public static final String IP_ADDRESSES = "ipAddresses";
    public static final String HOST_LOCATIONS = "locations";

    private static final String NULL_OBJECT_MSG = "Host cannot be null";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in Host";

    @Override
    public ObjectNode encode(Host host, CodecContext context) {
        checkNotNull(host, NULL_OBJECT_MSG);

        final JsonCodec<HostLocation> locationCodec =
                context.codec(HostLocation.class);
        // keep fields in string for compatibility
        final ObjectNode result = context.mapper().createObjectNode()
                .put(HOST_ID, host.id().toString())
                .put(MAC, host.mac().toString())
                .put(VLAN, host.vlan().toString())
                .put(IS_CONFIGURED, host.configured());

        final ArrayNode jsonIpAddresses = result.putArray(IP_ADDRESSES);
        for (final IpAddress ipAddress : host.ipAddresses()) {
            jsonIpAddresses.add(ipAddress.toString());
        }
        result.set(IP_ADDRESSES, jsonIpAddresses);

        final ArrayNode jsonLocations = result.putArray(HOST_LOCATIONS);
        for (final HostLocation location : host.locations()) {
            jsonLocations.add(locationCodec.encode(location, context));
        }
        result.set(HOST_LOCATIONS, jsonLocations);

        return annotate(result, host, context);
    }

    @Override
    public Host decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        MacAddress mac = MacAddress.valueOf(nullIsIllegal(
                json.get(MAC), MAC + MISSING_MEMBER_MESSAGE).asText());
        VlanId vlanId = VlanId.vlanId(nullIsIllegal(
                json.get(VLAN), VLAN + MISSING_MEMBER_MESSAGE).asText());
        HostId id = HostId.hostId(mac, vlanId);

        ArrayNode locationNodes = nullIsIllegal(
                (ArrayNode) json.get(HOST_LOCATIONS), HOST_LOCATIONS + MISSING_MEMBER_MESSAGE);
        Set<HostLocation> hostLocations =
                context.codec(HostLocation.class).decode(locationNodes, context)
                .stream().collect(Collectors.toSet());

        ArrayNode ipNodes = nullIsIllegal(
                (ArrayNode) json.get(IP_ADDRESSES), IP_ADDRESSES + MISSING_MEMBER_MESSAGE);
        Set<IpAddress> ips = new HashSet<>();
        ipNodes.forEach(ipNode -> {
            ips.add(IpAddress.valueOf(ipNode.asText()));
        });

        // check optional fields
        JsonNode configuredNode = json.get(IS_CONFIGURED);
        boolean configured = (null == configuredNode) ? false : configuredNode.asBoolean();

        Annotations annotations = extractAnnotations(json, context);

        return new DefaultHost(ProviderId.NONE, id, mac, vlanId, hostLocations,
                               ips, configured, annotations);
    }

}
