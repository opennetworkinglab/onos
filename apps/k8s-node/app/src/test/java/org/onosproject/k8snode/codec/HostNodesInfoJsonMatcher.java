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
package org.onosproject.k8snode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.k8snode.api.HostNodesInfo;

/**
 * Hamcrest matcher for HostNodesInfo.
 */
public final class HostNodesInfoJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final HostNodesInfo hostNodesInfo;

    private static final String HOST_IP = "hostIp";
    private static final String NODES = "nodes";

    private HostNodesInfoJsonMatcher(HostNodesInfo hostNodesInfo) {
        this.hostNodesInfo = hostNodesInfo;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check host IP
        String jsonHostIp = jsonNode.get(HOST_IP).asText();
        String hostIp = hostNodesInfo.hostIp().toString();
        if (!jsonHostIp.equals(hostIp)) {
            description.appendText("host IP was " + jsonHostIp);
            return false;
        }

        // check nodes
        JsonNode jsonNodes = jsonNode.get(NODES);
        if (jsonNodes.size() != hostNodesInfo.nodes().size()) {
            description.appendText("Nodes size was " + jsonNodes.size());
            return false;
        }

        boolean nodeFound = true;
        ArrayNode jsonNodeArray = (ArrayNode) jsonNodes;
        for (JsonNode jsonNodeTmp : jsonNodeArray) {
            if (!hostNodesInfo.nodes().contains(jsonNodeTmp.asText())) {
                nodeFound = false;
            }
        }

        if (!nodeFound) {
            description.appendText("Node not found");
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(hostNodesInfo.toString());
    }

    /**
     * Factory to allocate a hostNodesInfo matcher.
     *
     * @param info host IP address to nodes mapping info
     * @return matcher
     */
    public static HostNodesInfoJsonMatcher matchesHostNodesInfo(HostNodesInfo info) {
        return new HostNodesInfoJsonMatcher(info);
    }
}
