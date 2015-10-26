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
package org.onosproject.vtnrsc.web;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

import java.util.UUID;

import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.DefaultFlowClassifier;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.TenantId;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Flow Classifier JSON codec.
 */
public final class FlowClassifierCodec extends JsonCodec<FlowClassifier> {

    private static final String FLOW_CLASSIFIER_ID = "id";
    private static final String TENANT_ID = "tenant_id";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ETHER_TYPE = "etherType";
    private static final String PROTOCOL = "protocol";
    private static final String MIN_SRC_PORT_RANGE = "source_port_range_min";
    private static final String MAX_SRC_PORT_RANGE = "source_port_range_max";
    private static final String MIN_DST_PORT_RANGE = "destination_port_range_min";
    private static final String MAX_DST_PORT_RANGE = "destination_port_range_max";
    private static final String SRC_IP_PREFIX = "source_ip_prefix";
    private static final String DST_IP_PREFIX = "destination_ip_prefix";
    private static final String SRC_PORT = "logical_source_port";
    private static final String DST_PORT = "logical_destination_port";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in Flow Classifier.";

    @Override
    public FlowClassifier decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        FlowClassifier.Builder resultBuilder = new DefaultFlowClassifier.Builder();

        String flowClassifierId = nullIsIllegal(json.get(FLOW_CLASSIFIER_ID),
                FLOW_CLASSIFIER_ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setFlowClassifierId(FlowClassifierId.flowClassifierId(UUID.fromString(flowClassifierId)));

        String tenantId = nullIsIllegal(json.get(TENANT_ID), TENANT_ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setTenantId(TenantId.tenantId(tenantId));

        String flowClassiferName = nullIsIllegal(json.get(NAME), NAME + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setName(flowClassiferName);

        String flowClassiferDescription = nullIsIllegal(json.get(DESCRIPTION), DESCRIPTION + MISSING_MEMBER_MESSAGE)
                .asText();
        resultBuilder.setDescription(flowClassiferDescription);

        String etherType = nullIsIllegal(json.get(ETHER_TYPE), ETHER_TYPE + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setEtherType(etherType);

        String protocol = nullIsIllegal(json.get(PROTOCOL), PROTOCOL + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setProtocol(protocol);

        int minSrcPortRange = nullIsIllegal(json.get(MIN_SRC_PORT_RANGE), MIN_SRC_PORT_RANGE + MISSING_MEMBER_MESSAGE)
                .asInt();
        resultBuilder.setMinSrcPortRange(minSrcPortRange);

        int maxSrcPortRange = nullIsIllegal(json.get(MAX_SRC_PORT_RANGE), MAX_SRC_PORT_RANGE + MISSING_MEMBER_MESSAGE)
                .asInt();
        resultBuilder.setMaxSrcPortRange(maxSrcPortRange);

        int minDstPortRange = nullIsIllegal(json.get(MIN_DST_PORT_RANGE), MIN_DST_PORT_RANGE + MISSING_MEMBER_MESSAGE)
                .asInt();
        resultBuilder.setMinDstPortRange(minDstPortRange);

        int maxDstPortRange = nullIsIllegal(json.get(MAX_DST_PORT_RANGE), MAX_DST_PORT_RANGE + MISSING_MEMBER_MESSAGE)
                .asInt();
        resultBuilder.setMaxDstPortRange(maxDstPortRange);

        String srcIpPrefix = nullIsIllegal(json.get(SRC_IP_PREFIX), SRC_IP_PREFIX + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setSrcIpPrefix(IpPrefix.valueOf(srcIpPrefix));

        String dstIpPrefix = nullIsIllegal(json.get(DST_IP_PREFIX), DST_IP_PREFIX + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setDstIpPrefix(IpPrefix.valueOf(dstIpPrefix));

        String srcPort = nullIsIllegal(json.get(SRC_PORT), SRC_PORT + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setSrcPort(VirtualPortId.portId(srcPort));

        String dstPort = nullIsIllegal(json.get(DST_PORT), DST_PORT + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setDstPort(VirtualPortId.portId(dstPort));

        return resultBuilder.build();
    }

    @Override
    public ObjectNode encode(FlowClassifier flowClassifier, CodecContext context) {
        checkNotNull(flowClassifier, "flowClassifier cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("FLOW_CLASSIFIER_ID", flowClassifier.flowClassifierId().toString())
                .put("TENANT_ID", flowClassifier.tenantId().toString())
                .put("NAME", flowClassifier.name())
                .put("DESCRIPTION", flowClassifier.description())
                .put("ETHER_TYPE", flowClassifier.etherType())
                .put("PROTOCOL", flowClassifier.protocol())
                .put("MIN_SRC_PORT_RANGE", flowClassifier.minSrcPortRange())
                .put("MAX_SRC_PORT_RANGE", flowClassifier.maxSrcPortRange())
                .put("MIN_DST_PORT_RANGE", flowClassifier.minDstPortRange())
                .put("MAX_DST_PORT_RANGE", flowClassifier.maxDstPortRange())
                .put("SRC_IP_PREFIX", flowClassifier.srcIpPrefix().toString())
                .put("DST_IP_PREFIX", flowClassifier.dstIpPrefix().toString())
                .put("SRC_PORT", flowClassifier.srcPort().toString())
                .put("DST_PORT", flowClassifier.dstPort().toString());
        return result;
    }
}