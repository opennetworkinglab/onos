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
package org.onosproject.k8snode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.k8snode.api.DefaultK8sNode;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeInfo;
import org.onosproject.k8snode.api.K8sNodeState;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.k8snode.api.Constants.DEFAULT_CLUSTER_NAME;
import static org.onosproject.k8snode.api.Constants.DEFAULT_SEGMENT_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes node codec used for serializing and de-serializing JSON string.
 */
public final class K8sNodeCodec extends JsonCodec<K8sNode> {

    private final Logger log = getLogger(getClass());

    private static final String CLUSTER_NAME = "clusterName";
    private static final String HOSTNAME = "hostname";
    private static final String TYPE = "type";
    private static final String MODE = "mode";
    private static final String SEGMENT_ID = "segmentId";
    private static final String MANAGEMENT_IP = "managementIp";
    private static final String DATA_IP = "dataIp";
    private static final String NODE_IP = "nodeIp";
    private static final String NODE_MAC = "nodeMac";
    private static final String INTEGRATION_BRIDGE = "integrationBridge";
    private static final String EXTERNAL_BRIDGE = "externalBridge";
    private static final String LOCAL_BRIDGE = "localBridge";
    private static final String TUNNEL_BRIDGE = "tunnelBridge";
    private static final String STATE = "state";
    private static final String EXTERNAL_INTF = "externalInterface";
    private static final String EXTERNAL_BRIDGE_IP = "externalBridgeIp";
    private static final String EXTERNAL_GATEWAY_IP = "externalGatewayIp";
    private static final String EXTERNAL_GATEWAY_MAC = "externalGatewayMac";

    private static final String MISSING_MESSAGE = " is required in K8sNode";

    @Override
    public ObjectNode encode(K8sNode node, CodecContext context) {
        checkNotNull(node, "Kubernetes node cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(CLUSTER_NAME, node.clusterName())
                .put(HOSTNAME, node.hostname())
                .put(TYPE, node.type().name())
                .put(MODE, node.mode().name())
                .put(SEGMENT_ID, node.segmentId())
                .put(STATE, node.state().name())
                .put(MANAGEMENT_IP, node.managementIp().toString())
                .put(NODE_IP, node.nodeIp().toString());

        if (node.intgBridge() != null) {
            result.put(INTEGRATION_BRIDGE, node.intgBridge().toString());
        }

        if (node.extBridge() != null) {
            result.put(EXTERNAL_BRIDGE, node.extBridge().toString());
        }

        if (node.localBridge() != null) {
            result.put(LOCAL_BRIDGE, node.localBridge().toString());
        }

        if (node.tunBridge() != null) {
            result.put(TUNNEL_BRIDGE, node.tunBridge().toString());
        }

        if (node.dataIp() != null) {
            result.put(DATA_IP, node.dataIp().toString());
        }

        if (node.nodeMac() != null) {
            result.put(NODE_MAC, node.nodeMac().toString());
        }

        if (node.extIntf() != null) {
            result.put(EXTERNAL_INTF, node.extIntf());
        }

        if (node.extBridgeIp() != null) {
            result.put(EXTERNAL_BRIDGE_IP, node.extBridgeIp().toString());
        }

        if (node.extGatewayIp() != null) {
            result.put(EXTERNAL_GATEWAY_IP, node.extGatewayIp().toString());
        }

        if (node.extGatewayMac() != null) {
            result.put(EXTERNAL_GATEWAY_MAC, node.extGatewayMac().toString());
        }

        return result;
    }

    @Override
    public K8sNode decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String clusterName = json.get(CLUSTER_NAME).asText();

        if (StringUtils.isEmpty(clusterName)) {
            clusterName = DEFAULT_CLUSTER_NAME;
        }

        String hostname = nullIsIllegal(json.get(HOSTNAME).asText(),
                HOSTNAME + MISSING_MESSAGE);
        String type = nullIsIllegal(json.get(TYPE).asText(),
                TYPE + MISSING_MESSAGE);
        String mIp = nullIsIllegal(json.get(MANAGEMENT_IP).asText(),
                MANAGEMENT_IP + MISSING_MESSAGE);
        String nIp = nullIsIllegal(json.get(NODE_IP).asText(),
                NODE_IP + MISSING_MESSAGE);

        K8sNodeInfo nodeInfo = new K8sNodeInfo(IpAddress.valueOf(nIp), null);

        DefaultK8sNode.Builder nodeBuilder = DefaultK8sNode.builder()
                .clusterName(clusterName)
                .hostname(hostname)
                .type(K8sNode.Type.valueOf(type))
                .state(K8sNodeState.INIT)
                .managementIp(IpAddress.valueOf(mIp))
                .nodeInfo(nodeInfo);

        if (json.get(DATA_IP) != null) {
            nodeBuilder.dataIp(IpAddress.valueOf(json.get(DATA_IP).asText()));
        }

        JsonNode segmentIdJson = json.get(SEGMENT_ID);
        int segmentId = DEFAULT_SEGMENT_ID;
        if (segmentIdJson != null) {
            segmentId = segmentIdJson.asInt();
        }
        nodeBuilder.segmentId(segmentId);

        JsonNode intBridgeJson = json.get(INTEGRATION_BRIDGE);
        if (intBridgeJson != null) {
            nodeBuilder.intgBridge(DeviceId.deviceId(intBridgeJson.asText()));
        }

        JsonNode extBridgeJson = json.get(EXTERNAL_BRIDGE);
        if (extBridgeJson != null) {
            nodeBuilder.extBridge(DeviceId.deviceId(extBridgeJson.asText()));
        }

        JsonNode localBridgeJson = json.get(LOCAL_BRIDGE);
        if (localBridgeJson != null) {
            nodeBuilder.localBridge(DeviceId.deviceId(localBridgeJson.asText()));
        }

        JsonNode tunBridgeJson = json.get(TUNNEL_BRIDGE);
        if (tunBridgeJson != null) {
            nodeBuilder.tunBridge(DeviceId.deviceId(tunBridgeJson.asText()));
        }

        JsonNode extIntfJson = json.get(EXTERNAL_INTF);
        if (extIntfJson != null) {
            nodeBuilder.extIntf(extIntfJson.asText());
        }

        JsonNode extBridgeIpJson = json.get(EXTERNAL_BRIDGE_IP);
        if (extBridgeIpJson != null) {
            nodeBuilder.extBridgeIp(IpAddress.valueOf(extBridgeIpJson.asText()));
        }

        JsonNode extGatewayIpJson = json.get(EXTERNAL_GATEWAY_IP);
        if (extGatewayIpJson != null) {
            nodeBuilder.extGatewayIp(IpAddress.valueOf(extGatewayIpJson.asText()));
        }

        JsonNode extGatewayMacJson = json.get(EXTERNAL_GATEWAY_MAC);
        if (extGatewayMacJson != null) {
            nodeBuilder.extGatewayMac(MacAddress.valueOf(extGatewayMacJson.asText()));
        }

        log.trace("node is {}", nodeBuilder.build().toString());

        return nodeBuilder.build();
    }
}
