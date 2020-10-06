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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.k8snode.api.K8sNode;

/**
 * Hamcrest matcher for kubernetes node.
 */
public final class K8sNodeJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final K8sNode node;

    private static final String CLUSTER_NAME = "clusterName";
    private static final String HOSTNAME = "hostname";
    private static final String TYPE = "type";
    private static final String SEGMENT_ID = "segmentId";
    private static final String MANAGEMENT_IP = "managementIp";
    private static final String DATA_IP = "dataIp";
    private static final String NODE_IP = "nodeIp";
    private static final String INTEGRATION_BRIDGE = "integrationBridge";
    private static final String STATE = "state";
    private static final String EXTERNAL_INTF = "externalInterface";
    private static final String EXTERNAL_BRIDGE_IP = "externalBridgeIp";
    private static final String EXTERNAL_GATEWAY_IP = "externalGatewayIp";
    private static final String EXTERNAL_GATEWAY_MAC = "externalGatewayMac";

    private K8sNodeJsonMatcher(K8sNode node) {
        this.node = node;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check cluster name
        String jsonClusterName = jsonNode.get(CLUSTER_NAME).asText();
        String clusterName = node.clusterName();
        if (!jsonClusterName.equals(clusterName)) {
            description.appendText("cluster name was " + jsonClusterName);
            return false;
        }

        // check hostname
        String jsonHostname = jsonNode.get(HOSTNAME).asText();
        String hostname = node.hostname();
        if (!jsonHostname.equals(hostname)) {
            description.appendText("hostname was " + jsonHostname);
            return false;
        }

        // check type
        String jsonType = jsonNode.get(TYPE).asText();
        String type = node.type().name();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check segment ID
        JsonNode jsonSegmentId = jsonNode.get(SEGMENT_ID);
        if (jsonSegmentId != null) {
            int segmentId = jsonSegmentId.asInt();
            if (segmentId != node.segmentId()) {
                description.appendText("segment ID was " + segmentId);
                return false;
            }
        }

        // check management IP
        String jsonMgmtIp = jsonNode.get(MANAGEMENT_IP).asText();
        String mgmtIp = node.managementIp().toString();
        if (!jsonMgmtIp.equals(mgmtIp)) {
            description.appendText("management IP was " + jsonMgmtIp);
            return false;
        }

        // check node IP
        String jsonNodeIp = jsonNode.get(NODE_IP).asText();
        String nodeIp = node.nodeIp().toString();
        if (!jsonNodeIp.equals(nodeIp)) {
            description.appendText("node IP was " + jsonNodeIp);
            return false;
        }

        // check integration bridge
        JsonNode jsonIntgBridge = jsonNode.get(INTEGRATION_BRIDGE);
        if (jsonIntgBridge != null) {
            String intgBridge = node.intgBridge().toString();
            if (!jsonIntgBridge.asText().equals(intgBridge)) {
                description.appendText("integration bridge was " + jsonIntgBridge);
                return false;
            }
        }

        // check state
        String jsonState = jsonNode.get(STATE).asText();
        String state = node.state().name();
        if (!jsonState.equals(state)) {
            description.appendText("state was " + jsonState);
            return false;
        }

        // check data IP
        JsonNode jsonDataIp = jsonNode.get(DATA_IP);
        if (jsonDataIp != null) {
            String dataIp = node.dataIp().toString();
            if (!jsonDataIp.asText().equals(dataIp)) {
                description.appendText("Data IP was " + jsonDataIp.asText());
                return false;
            }
        }

        // check external interface
        JsonNode jsonExtIntf = jsonNode.get(EXTERNAL_INTF);
        if (jsonExtIntf != null) {
            String extIntf = node.extIntf();
            if (!jsonExtIntf.asText().equals(extIntf)) {
                description.appendText("External interface was " + jsonExtIntf.asText());
                return false;
            }
        }

        // check external bridge IP
        JsonNode jsonExtBridgeIp = jsonNode.get(EXTERNAL_BRIDGE_IP);
        if (jsonExtBridgeIp != null) {
            IpAddress extBridgeIp = node.extBridgeIp();
            if (!jsonExtBridgeIp.asText().equals(extBridgeIp.toString())) {
                description.appendText("External bridge IP was " + jsonExtBridgeIp.asText());
                return false;
            }
        }

        // check external gateway IP
        JsonNode jsonExtGatewayIp = jsonNode.get(EXTERNAL_GATEWAY_IP);
        if (jsonExtGatewayIp != null) {
            IpAddress extGatewayIp = node.extGatewayIp();
            if (!jsonExtGatewayIp.asText().equals(extGatewayIp.toString())) {
                description.appendText("External gateway IP was " + jsonExtGatewayIp.asText());
                return false;
            }
        }

        // check external gateway MAC
        JsonNode jsonExtGatewayMac = jsonNode.get(EXTERNAL_GATEWAY_MAC);
        if (jsonExtGatewayMac != null) {
            MacAddress extGatewayMac = node.extGatewayMac();
            if (!jsonExtGatewayMac.asText().equals(extGatewayMac.toString())) {
                description.appendText("External gateway MAC was " + jsonExtGatewayMac.asText());
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(node.toString());
    }

    /**
     * Factory to allocate an kubernetes node matcher.
     *
     * @param node kubernetes node object we are looking for
     * @return matcher
     */
    public static K8sNodeJsonMatcher matchesK8sNode(K8sNode node) {
        return new K8sNodeJsonMatcher(node);
    }
}
