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
package org.onosproject.vtnweb.web;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.DefaultFlowClassifier;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Flow Classifier JSON codec.
 */
public final class FlowClassifierCodec extends JsonCodec<FlowClassifier> {

    private static final String FLOW_CLASSIFIER_ID = "id";
    private static final String TENANT_ID = "tenant_id";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ETHER_TYPE = "ethertype";
    private static final String PROTOCOL = "protocol";
    private static final String PRIORITY = "priority";
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
        resultBuilder.setFlowClassifierId(FlowClassifierId.of(flowClassifierId));

        String tenantId = nullIsIllegal(json.get(TENANT_ID), TENANT_ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setTenantId(TenantId.tenantId(tenantId));

        String flowClassiferName = nullIsIllegal(json.get(NAME), NAME + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setName(flowClassiferName);

        String flowClassiferDescription = (json.get(DESCRIPTION)).asText();
        resultBuilder.setDescription(flowClassiferDescription);

        String etherType = nullIsIllegal(json.get(ETHER_TYPE), ETHER_TYPE + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setEtherType(etherType);

        if (json.get(PROTOCOL) != null && !"null".equals((json.get(PROTOCOL)).asText())) {
            String protocol = (json.get(PROTOCOL)).asText();
            resultBuilder.setProtocol(protocol);
        }

        if (json.get(PRIORITY) != null && !"null".equals((json.get(PRIORITY)).asText())) {
            int priority = (json.get(PRIORITY)).asInt();
            resultBuilder.setPriority(priority);
        }

        int minSrcPortRange = (json.get(MIN_SRC_PORT_RANGE)).asInt();
        resultBuilder.setMinSrcPortRange(minSrcPortRange);

        int maxSrcPortRange = (json.get(MAX_SRC_PORT_RANGE)).asInt();
        resultBuilder.setMaxSrcPortRange(maxSrcPortRange);

        int minDstPortRange = (json.get(MIN_DST_PORT_RANGE)).asInt();
        resultBuilder.setMinDstPortRange(minDstPortRange);

        int maxDstPortRange = (json.get(MAX_DST_PORT_RANGE)).asInt();
        resultBuilder.setMaxDstPortRange(maxDstPortRange);

        if (json.get(SRC_IP_PREFIX) != null && !"null".equals((json.get(SRC_IP_PREFIX)).asText())) {
            String srcIpPrefix = (json.get(SRC_IP_PREFIX)).asText();
            resultBuilder.setSrcIpPrefix(IpPrefix.valueOf(srcIpPrefix));
        }

        if (json.get(DST_IP_PREFIX) != null && !"null".equals((json.get(DST_IP_PREFIX)).asText())) {
            String dstIpPrefix = (json.get(DST_IP_PREFIX)).asText();
            resultBuilder.setDstIpPrefix(IpPrefix.valueOf(dstIpPrefix));
        }

        if (json.get(SRC_PORT) != null && !"null".equals((json.get(SRC_PORT)).asText())) {
            String srcPort = (json.get(SRC_PORT)).asText();
            resultBuilder.setSrcPort(VirtualPortId.portId(srcPort));
        }

        if (json.get(DST_PORT) != null && !"null".equals((json.get(DST_PORT)).asText())) {
            String dstPort = (json.get(DST_PORT)).asText();
            resultBuilder.setDstPort(VirtualPortId.portId(dstPort));
        }
        return resultBuilder.build();
    }

    @Override
    public ObjectNode encode(FlowClassifier flowClassifier, CodecContext context) {
        checkNotNull(flowClassifier, "flowClassifier cannot be null");
        ObjectNode result = context.mapper().createObjectNode();
        result.put(FLOW_CLASSIFIER_ID, flowClassifier.flowClassifierId().toString())
                .put(TENANT_ID, flowClassifier.tenantId().toString())
                .put(NAME, flowClassifier.name())
                .put(DESCRIPTION, flowClassifier.description())
                .put(ETHER_TYPE, flowClassifier.etherType())
                .put(PROTOCOL, flowClassifier.protocol())
                .put(PRIORITY, flowClassifier.priority())
                .put(MIN_SRC_PORT_RANGE, flowClassifier.minSrcPortRange())
                .put(MAX_SRC_PORT_RANGE, flowClassifier.maxSrcPortRange())
                .put(MIN_DST_PORT_RANGE, flowClassifier.minDstPortRange())
                .put(MAX_DST_PORT_RANGE, flowClassifier.maxDstPortRange());

        if (flowClassifier.srcIpPrefix() != null) {
            result.put(SRC_IP_PREFIX, flowClassifier.srcIpPrefix().toString());
        } else {
            result.put(SRC_IP_PREFIX, "null");
        }
        if (flowClassifier.dstIpPrefix() != null) {
            result.put(DST_IP_PREFIX, flowClassifier.dstIpPrefix().toString());
        } else {
            result.put(DST_IP_PREFIX, "null");
        }

        if (flowClassifier.srcPort() != null) {
            result.put(SRC_PORT, flowClassifier.srcPort().toString());
        } else {
            result.put(SRC_PORT, "null");
        }
        if (flowClassifier.dstPort() != null) {
            result.put(DST_PORT, flowClassifier.dstPort().toString());
        } else {
            result.put(DST_PORT, "null");
        }
        return result;
    }
}
