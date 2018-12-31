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
package org.onosproject.openstacktelemetry.codec.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.api.DefaultFlowInfo;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.VlanId.NO_VID;

/**
 * Openstack telemetry codec used for serializing and de-serializing JSON string.
 */
public final class FlowInfoJsonCodec extends JsonCodec<FlowInfo> {

    private static final String FLOW_TYPE = "flowType";
    private static final String DEVICE_ID = "deviceId";
    private static final String INPUT_INTERFACE_ID = "inputInterfaceId";
    private static final String OUTPUT_INTERFACE_ID = "outputInterfaceId";

    private static final String VLAN_ID = "vlanId";
    private static final String VXLAN_ID = "vxlanId";
    private static final String SRC_IP = "srcIp";
    private static final String SRC_IP_PREFIX_LEN = "srcIpPrefixLength";
    private static final String DST_IP = "dstIp";
    private static final String DST_IP_PREFIX_LEN = "dstIpPrefixLength";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";
    private static final String PROTOCOL = "protocol";
    private static final String SRC_MAC = "srcMac";
    private static final String DST_MAC = "dstMac";
    private static final String STATS_INFO = "statsInfo";

    @Override
    public ObjectNode encode(FlowInfo info, CodecContext context) {
        checkNotNull(info, "FlowInfo cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(FLOW_TYPE, info.flowType())
                .put(DEVICE_ID, info.deviceId().toString())
                .put(INPUT_INTERFACE_ID, info.inputInterfaceId())
                .put(OUTPUT_INTERFACE_ID, info.outputInterfaceId())
                .put(SRC_IP, info.srcIp().address().toString())
                .put(SRC_IP_PREFIX_LEN, info.srcIp().prefixLength())
                .put(DST_IP, info.dstIp().address().toString())
                .put(DST_IP_PREFIX_LEN, info.dstIp().prefixLength())
                .put(SRC_PORT, info.srcPort().toString())
                .put(DST_PORT, info.dstPort().toString())
                .put(PROTOCOL, info.protocol())
                .put(SRC_MAC, info.srcMac().toString())
                .put(DST_MAC, info.dstMac().toString());

        if (info.vlanId() != null) {
            result.put(VLAN_ID, info.vlanId().toString());
        } else {
            result.put(VXLAN_ID, info.vxlanId());
        }

        ObjectNode statsInfoJson =
                context.codec(StatsInfo.class).encode(info.statsInfo(), context);

        result.put(STATS_INFO, statsInfoJson);

        return result;
    }

    @Override
    public FlowInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String flowType = json.get(FLOW_TYPE).asText();
        String deviceId = json.get(DEVICE_ID).asText();
        int inputInterfaceId = json.get(INPUT_INTERFACE_ID).asInt();
        int outputInterfaceId = json.get(OUTPUT_INTERFACE_ID).asInt();
        String srcIp = json.get(SRC_IP).asText();
        int srcIpPrefixLength = json.get(SRC_IP_PREFIX_LEN).asInt();
        String dstIp = json.get(DST_IP).asText();
        int dstIpPrefixLength = json.get(DST_IP_PREFIX_LEN).asInt();
        int srcPort = json.get(SRC_PORT).asInt();
        int dstPort = json.get(DST_PORT).asInt();
        String protocol = json.get(PROTOCOL).asText();
        String srcMac = json.get(SRC_MAC).asText();
        String dstMac = json.get(DST_MAC).asText();

        VlanId vlanId;
        short vxlanId = 0;
        try {
            if (json.get(VLAN_ID).isNull()) {
                vlanId = VlanId.vlanId(NO_VID);
                if (!(json.get(VXLAN_ID).isNull())) {
                    vxlanId = (short) json.get(VXLAN_ID).asInt();
                }
            } else {
                vlanId = VlanId.vlanId((short) json.get(VLAN_ID).asInt());
            }
        } catch (NullPointerException ex) {
            vlanId = VlanId.vlanId();
        }

        JsonNode statsInfoJson = json.get(STATS_INFO);

        JsonCodec<StatsInfo> statsInfoCodec = context.codec(StatsInfo.class);
        StatsInfo statsInfo = statsInfoCodec.decode((ObjectNode) statsInfoJson.deepCopy(), context);

        return new DefaultFlowInfo.DefaultBuilder()
                .withFlowType(Byte.valueOf(flowType))
                .withDeviceId(DeviceId.deviceId(deviceId))
                .withInputInterfaceId(inputInterfaceId)
                .withOutputInterfaceId(outputInterfaceId)
                .withSrcIp(IpPrefix.valueOf(IpAddress.valueOf(srcIp), srcIpPrefixLength))
                .withDstIp(IpPrefix.valueOf(IpAddress.valueOf(dstIp), dstIpPrefixLength))
                .withSrcPort(TpPort.tpPort(srcPort))
                .withDstPort(TpPort.tpPort(dstPort))
                .withProtocol(Byte.valueOf(protocol))
                .withSrcMac(MacAddress.valueOf(srcMac))
                .withDstMac(MacAddress.valueOf(dstMac))
                .withVlanId(vlanId)
                .withVxlanId(vxlanId)
                .withStatsInfo(statsInfo)
                .build();
    }
}
