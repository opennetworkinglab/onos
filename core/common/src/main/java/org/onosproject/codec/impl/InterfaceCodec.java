/*
 * Copyright 2020-present Open Networking Foundation
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
import com.google.common.collect.Lists;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.slf4j.Logger;


import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Interface JSON codec.
 */
public final class InterfaceCodec extends JsonCodec<Interface> {
    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String NAME = "name";
    private static final String CONNECT_POINT = "connect point";
    private static final String IPS = "ips";
    private static final String MAC = "mac";
    private static final String VLAN = "vlan";
    private static final String VLAN_UNTAGGED = "vlan Untagged";
    private static final String VLAN_TAGGED = "vlan Tagged";
    private static final String VLAN_NATIVE = "vlan Native";
    private static final String MISSING_NAME_MESSAGE =
            " name is required in Interface";
    private static final String MISSING_CONNECT_POINT_MESSAGE =
            " connect point is required in Interface";

    @Override
    public ObjectNode encode(Interface interf, CodecContext context) {
        checkNotNull(interf, "Interfaces cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(NAME, interf.name())
                .put(CONNECT_POINT, interf.connectPoint().toString())
                .put(IPS, interf.ipAddressesList().toString())
                .put(MAC, interf.mac().toString())
                .put(VLAN, interf.vlan().toString())
                .put(VLAN_UNTAGGED, interf.vlanUntagged().toString())
                .put(VLAN_TAGGED, interf.vlanTagged().toString())
                .put(VLAN_NATIVE, interf.vlanNative().toString());
        return result;
    }
    @Override
    public Interface decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String name = nullIsIllegal(json.findValue(NAME),
                NAME + MISSING_NAME_MESSAGE).asText();
        ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(nullIsIllegal(json.findValue(CONNECT_POINT),
                CONNECT_POINT + MISSING_CONNECT_POINT_MESSAGE).asText());
        List<InterfaceIpAddress> ipAddresses = Lists.newArrayList();
        if (json.findValue(IPS) != null) {
            json.findValue(IPS).forEach(ip -> {
                ipAddresses.add(InterfaceIpAddress.valueOf(ip.asText()));
            });
        }

        MacAddress macAddr =  json.findValue(MAC) == null ?
                null : MacAddress.valueOf(json.findValue(MAC).asText());
        VlanId vlanId =  json.findValue(VLAN) == null ?
                VlanId.NONE : VlanId.vlanId(Short.parseShort(json.findValue(VLAN).asText()));
        Interface inter = new Interface(
                name,
                connectPoint,
                ipAddresses,
                macAddr,
                vlanId);

        return inter;
    }

}

