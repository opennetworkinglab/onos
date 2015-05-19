/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.segmentrouting.web;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.net.IpProtocol;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.segmentrouting.Policy;
import org.onosproject.segmentrouting.TunnelPolicy;

public final class PolicyCodec extends JsonCodec<Policy> {

    // JSON field names
    private static final String POLICY_ID = "policy_id";
    private static final String PRIORITY = "priority";
    private static final String TYPE = "policy_type";
    private static final String TUNNEL_ID = "tunnel_id";
    private static final String DST_IP = "dst_ip";
    private static final String SRC_IP = "src_ip";
    private static final String PROTO_TYPE = "proto_type";
    private static final String SRC_PORT = "src_tp_port";
    private static final String DST_PORT = "dst_tp_port";

    @Override
    public ObjectNode encode(Policy policy, CodecContext context) {
        final ObjectNode result = context.mapper().createObjectNode()
                .put(POLICY_ID, policy.id());

        result.put(PRIORITY, policy.priority());
        result.put(TYPE, policy.type().toString());

        if (policy.selector().getCriterion(Criterion.Type.IPV4_DST) != null) {
            IPCriterion criterion = (IPCriterion) policy.selector().getCriterion(
                    Criterion.Type.IPV4_DST);
            result.put(DST_IP, criterion.ip().toString());
        }
        if (policy.selector().getCriterion(Criterion.Type.IPV4_SRC) != null) {
            IPCriterion criterion = (IPCriterion) policy.selector().getCriterion(
                    Criterion.Type.IPV4_SRC);
            result.put(SRC_IP, criterion.ip().toString());
        }
        if (policy.selector().getCriterion(Criterion.Type.IP_PROTO) != null) {
            IPProtocolCriterion protocolCriterion =
                    (IPProtocolCriterion) policy.selector().getCriterion(Criterion.Type.IP_PROTO);
            result.put(PROTO_TYPE, protocolCriterion.protocol());
        }
        if (policy.selector().getCriterion(Criterion.Type.TCP_SRC) != null) {
            TcpPortCriterion tcpPortCriterion =
                    (TcpPortCriterion) policy.selector().getCriterion(Criterion.Type.TCP_SRC);
            result.put(SRC_PORT, tcpPortCriterion.toString());
        } else if (policy.selector().getCriterion(Criterion.Type.UDP_SRC) != null) {
            UdpPortCriterion udpPortCriterion =
                    (UdpPortCriterion) policy.selector().getCriterion(Criterion.Type.UDP_SRC);
            result.put(SRC_PORT, udpPortCriterion.toString());
        }
        if (policy.selector().getCriterion(Criterion.Type.TCP_DST) != null) {
            TcpPortCriterion tcpPortCriterion =
                    (TcpPortCriterion) policy.selector().getCriterion(Criterion.Type.TCP_DST);
            result.put(DST_PORT, tcpPortCriterion.toString());
        } else if (policy.selector().getCriterion(Criterion.Type.UDP_DST) != null) {
            UdpPortCriterion udpPortCriterion =
                    (UdpPortCriterion) policy.selector().getCriterion(Criterion.Type.UDP_DST);
            result.put(DST_PORT, udpPortCriterion.toString());
        }
        if (policy.selector().getCriterion(Criterion.Type.IP_PROTO) != null) {
            IPProtocolCriterion protocolCriterion =
                    (IPProtocolCriterion) policy.selector().getCriterion(Criterion.Type.IP_PROTO);
            result.put(PROTO_TYPE, protocolCriterion.toString());
        }

        if (policy.type() == Policy.Type.TUNNEL_FLOW) {
            result.put(TUNNEL_ID, ((TunnelPolicy) policy).tunnelId());
        }

        return result;
    }

    @Override
    public Policy decode(ObjectNode json, CodecContext context) {

        String pid = json.path(POLICY_ID).asText();
        String type = json.path(TYPE).asText();
        int priority = json.path(PRIORITY).asInt();
        String dstIp = json.path(DST_IP).asText();
        String srcIp = json.path(SRC_IP).asText();
        String tunnelId = json.path(TUNNEL_ID).asText();
        String protoType = json.path(PROTO_TYPE).asText();
        short srcPort = json.path(SRC_PORT).shortValue();
        short dstPort = json.path(DST_PORT).shortValue();

        if (tunnelId != null) {
            TrafficSelector.Builder tsb = DefaultTrafficSelector.builder();
            tsb.matchEthType(Ethernet.TYPE_IPV4);
            if (dstIp != null && !dstIp.isEmpty()) {
                tsb.matchIPDst(IpPrefix.valueOf(dstIp));
            }
            if (srcIp != null && !srcIp.isEmpty()) {
                tsb.matchIPSrc(IpPrefix.valueOf(srcIp));
            }
            if (protoType != null && !protoType.isEmpty()) {
                Short ipProto = Short.valueOf(IpProtocol.valueOf(protoType).value());
                tsb.matchIPProtocol(ipProto.byteValue());
                if (IpProtocol.valueOf(protoType).equals(IpProtocol.TCP)) {
                    if (srcPort != 0) {
                        tsb.matchTcpSrc(srcPort);
                    }
                    if (dstPort != 0) {
                        tsb.matchTcpDst(dstPort);
                    }
                } else if (IpProtocol.valueOf(protoType).equals(IpProtocol.UDP)) {
                    if (srcPort != 0) {
                        tsb.matchUdpSrc(srcPort);
                    }
                    if (dstPort != 0) {
                        tsb.matchUdpDst(dstPort);
                    }
                }
            }
            TunnelPolicy.Builder tpb = TunnelPolicy.builder().setPolicyId(pid);
            if (tunnelId != null) {
                tpb.setTunnelId(tunnelId);
            }
            if (!json.path(PRIORITY).isMissingNode()) {
                tpb.setPriority(priority);
            }
            if (!json.path(TYPE).isMissingNode()) {
                tpb.setType(Policy.Type.valueOf(type));
            }
            tpb.setSelector(tsb.build());

            return tpb.build();
        } else {
            // TODO: handle more policy types
            return null;
        }


    }

}
