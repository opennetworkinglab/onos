/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.segmentrouting.Policy;
import org.onosproject.segmentrouting.TunnelPolicy;

/**
 * Codec of Policy class.
 */
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

        if (policy.dstIp() != null) {
            result.put(DST_IP, policy.dstIp());
        }
        if (policy.srcIp() != null) {
            result.put(SRC_IP, policy.srcIp());
        }
        if (policy.ipProto() != null) {
            result.put(PROTO_TYPE, policy.ipProto());
        }

        int srcPort = policy.srcPort() & 0xffff;
        if (policy.srcPort() != 0) {
            result.put(SRC_PORT, srcPort);
        }
        int dstPort = policy.dstPort() & 0xffff;
        if (policy.dstPort() != 0) {
            result.put(DST_PORT, dstPort);
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

        if (json.path(POLICY_ID).isMissingNode() || pid == null) {
            // TODO: handle errors
            return null;
        }

        TunnelPolicy.Builder tpb = TunnelPolicy.builder().setPolicyId(pid);
        if (!json.path(TYPE).isMissingNode() && type != null &&
                Policy.Type.valueOf(type).equals(Policy.Type.TUNNEL_FLOW)) {

            if (json.path(TUNNEL_ID).isMissingNode() || tunnelId == null) {
                return null;
            }

            tpb.setTunnelId(tunnelId);
            tpb.setType(Policy.Type.valueOf(type));

            if (!json.path(PRIORITY).isMissingNode()) {
                tpb.setPriority(priority);
            }
            if (dstIp != null) {
                tpb.setDstIp(dstIp);
            }
            if (srcIp != null) {
                tpb.setSrcIp(srcIp);
            }
            if (protoType != null) {
                tpb.setIpProto(protoType);
            }
            if (dstPort != 0) {
                tpb.setDstPort(dstPort);
            }
            if (srcPort != 0) {
                tpb.setSrcPort(srcPort);
            }
        }

        return tpb.build();
    }

}
