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
package org.onosproject.openstackvtap.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapCriterion;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getProtocolStringFromType;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getProtocolTypeFromString;

/**
 * Openstack vTap codec used for serializing and de-serializing JSON string.
 */
public final class OpenstackVtapCriterionCodec extends JsonCodec<OpenstackVtapCriterion> {

    private static final String SRC_IP = "srcIp";
    private static final String DST_IP = "dstIp";
    private static final String IP_PROTOCOL = "ipProto";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";

    private static final String MISSING_MESSAGE = " is required in OpenstackVtapCriterion";

    @Override
    public ObjectNode encode(OpenstackVtapCriterion entity, CodecContext context) {
        String protoStr = getProtocolStringFromType(entity.ipProtocol());

        return context.mapper().createObjectNode()
                .put(SRC_IP, entity.srcIpPrefix().address().toString())
                .put(DST_IP, entity.dstIpPrefix().address().toString())
                .put(IP_PROTOCOL, protoStr)
                .put(SRC_PORT, entity.srcTpPort().toString())
                .put(DST_PORT, entity.dstTpPort().toString());
    }

    @Override
    public OpenstackVtapCriterion decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        OpenstackVtapCriterion.Builder cBuilder = DefaultOpenstackVtapCriterion.builder();

        // parse source IP address
        IpPrefix srcIp = IpPrefix.valueOf(IpAddress.valueOf(nullIsIllegal(
                json.get(SRC_IP).asText(), SRC_IP + MISSING_MESSAGE)), 32);
        // parse destination IP address
        IpPrefix dstIp = IpPrefix.valueOf(IpAddress.valueOf(nullIsIllegal(
                json.get(DST_IP).asText(), DST_IP + MISSING_MESSAGE)), 32);

        cBuilder.srcIpPrefix(srcIp);
        cBuilder.dstIpPrefix(dstIp);

        // parse IP protocol
        String ipProtoStr = json.get(IP_PROTOCOL).asText();
        if (ipProtoStr != null) {
            cBuilder.ipProtocol(getProtocolTypeFromString(ipProtoStr));
        }

        // parse source port number
        int srcPort = json.get(SRC_PORT).asInt(0);

        // parse destination port number
        int dstPort = json.get(DST_PORT).asInt(0);

        cBuilder.srcTpPort(TpPort.tpPort(srcPort));
        cBuilder.dstTpPort(TpPort.tpPort(dstPort));

        return cBuilder.build();
    }
}
