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
package org.onosproject.kubevirtnode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
import org.onosproject.kubevirtnode.api.Constants;

/**
 * Hamcrest matcher for kubevirt node.
 */
public final class KubevirtNodeJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtNode node;
    private static final String INTEGRATION_BRIDGE = "integrationBridge";
    private static final String TUNNEL_BRIDGE = "tunnelBridge";
    private static final String STATE = "state";
    private static final String PHYSICAL_INTERFACES = "phyIntfs";

    private KubevirtNodeJsonMatcher(KubevirtNode node) {
        this.node = node;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check hostname
        String jsonHostname = jsonNode.get(Constants.HOST_NAME).asText();
        String hostname = node.hostname();
        if (!jsonHostname.equals(hostname)) {
            description.appendText("hostname was " + jsonHostname);
            return false;
        }

        // check type
        String jsonType = jsonNode.get(Constants.TYPE).asText();
        String type = node.type().name();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check management IP
        String jsonMgmtIp = jsonNode.get(Constants.MANAGEMENT_IP).asText();
        String mgmtIp = node.managementIp().toString();
        if (!jsonMgmtIp.equals(mgmtIp)) {
            description.appendText("management IP was " + jsonMgmtIp);
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

        // check tunnel bridge
        JsonNode jsonTunBridge = jsonNode.get(TUNNEL_BRIDGE);
        if (jsonTunBridge != null) {
            String tunBridge = node.tunBridge().toString();
            if (!jsonTunBridge.asText().equals(tunBridge)) {
                description.appendText("tunnel bridge was " + jsonTunBridge);
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
        JsonNode jsonDataIp = jsonNode.get(Constants.DATA_IP);
        if (jsonDataIp != null) {
            String dataIp = node.dataIp().toString();
            if (!jsonDataIp.asText().equals(dataIp)) {
                description.appendText("Data IP was " + jsonDataIp.asText());
                return false;
            }
        }

        // check physical interfaces
        JsonNode jsonPhyIntfs = jsonNode.get(PHYSICAL_INTERFACES);
        if (jsonPhyIntfs != null) {
            if (jsonPhyIntfs.size() != node.phyIntfs().size()) {
                description.appendText("physical interface size was " + jsonPhyIntfs.size());
                return false;
            }

            for (KubevirtPhyInterface phyIntf : node.phyIntfs()) {
                boolean intfFound = false;
                for (int intfIndex = 0; intfIndex < jsonPhyIntfs.size(); intfIndex++) {
                    KubevirtPhyInterfaceJsonMatcher intfMatcher =
                            KubevirtPhyInterfaceJsonMatcher.matchesKubevirtPhyInterface(phyIntf);
                    if (intfMatcher.matches(jsonPhyIntfs.get(intfIndex))) {
                        intfFound = true;
                        break;
                    }
                }

                if (!intfFound) {
                    description.appendText("PhyIntf not found " + phyIntf.toString());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(node.toString());
    }

    /**
     * Factory to allocate an kubevirt node matcher.
     *
     * @param node kubevirt node object we are looking for
     * @return matcher
     */
    public static KubevirtNodeJsonMatcher matchesKubevirtNode(KubevirtNode node) {
        return new KubevirtNodeJsonMatcher(node);
    }
}
