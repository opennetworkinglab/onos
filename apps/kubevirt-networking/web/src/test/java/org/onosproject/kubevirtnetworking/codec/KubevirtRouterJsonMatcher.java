/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;

/**
 * Hamcrest matcher for kubevirt router interface.
 */
public final class KubevirtRouterJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtRouter router;
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ENABLE_SNAT = "enableSnat";
    private static final String INTERNAL = "internal";
    private static final String EXTERNAL = "external";
    private static final String PEER_ROUTER = "peerRouter";
    private static final String IP_ADDRESS = "ip";
    private static final String MAC_ADDRESS = "mac";
    private static final String NETWORK = "network";

    private KubevirtRouterJsonMatcher(KubevirtRouter router) {
        this.router = router;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check name
        String jsonName = jsonNode.get(NAME).asText();
        String name = router.name();
        if (!jsonName.equals(name)) {
            description.appendText("Name was " + jsonName);
            return false;
        }

        // check description
        JsonNode jsonDescription = jsonNode.get(DESCRIPTION);
        if (jsonDescription != null) {
            String myDescription = router.description();
            if (!jsonDescription.asText().equals(myDescription)) {
                description.appendText("Description was " + jsonDescription);
                return false;
            }
        }

        // check enable snat
        JsonNode jsonEnableSnat = jsonNode.get(ENABLE_SNAT);
        if (jsonEnableSnat != null) {
            boolean enableSnat = router.enableSnat();
            if (jsonEnableSnat.asBoolean() != enableSnat) {
                description.appendText("EnableSNAT was " + jsonEnableSnat);
                return false;
            }
        }

        // check vrouter MAC
        String jsonMac = jsonNode.get(MAC_ADDRESS).asText();
        String mac = router.mac().toString();
        if (!jsonMac.equals(mac)) {
            description.appendText("MAC was " + jsonMac);
            return false;
        }

        // check internal
        JsonNode jsonInternal = jsonNode.get(INTERNAL);
        if (jsonInternal != null) {
            if (jsonInternal.size() != router.internal().size()) {
                description.appendText("Internal networks size was " + jsonInternal.size());
                return false;
            }

            for (int networkIndex = 0; networkIndex < jsonInternal.size(); networkIndex++) {
                boolean networkFound = false;
                String jsonNetwork = jsonInternal.get(networkIndex).asText();
                if (router.internal().contains(jsonNetwork)) {
                    networkFound = true;
                }

                if (!networkFound) {
                    description.appendText("network not found " + jsonNetwork);
                    return false;
                }
            }
        }

        // check external
        ArrayNode jsonExternal = (ArrayNode) jsonNode.get(EXTERNAL);
        if (jsonExternal != null) {
            if (jsonExternal.size() != router.external().size()) {
                description.appendText("External networks size was " + jsonExternal.size());
                return false;
            }

            for (int itemIndex = 0; itemIndex < jsonExternal.size(); itemIndex++) {
                boolean itemFound = false;
                ObjectNode jsonItem = (ObjectNode) jsonExternal.get(itemIndex);
                String jsonIp = jsonItem.get(IP_ADDRESS).asText();
                String jsonNetwork = jsonItem.get(NETWORK).asText();

                if (router.external().containsKey(jsonIp)) {
                    if (router.external().get(jsonIp).equals(jsonNetwork)) {
                        itemFound = true;
                    }
                }

                if (!itemFound) {
                    description.appendText("External not found " + jsonItem.toString());
                    return false;
                }
            }
        }

        // check peer router
        ObjectNode jsonPeerRouter = (ObjectNode) jsonNode.get(PEER_ROUTER);
        if (jsonPeerRouter != null) {
            JsonNode jsonIp = jsonPeerRouter.get(IP_ADDRESS);

            if (jsonIp != null) {
                if (!jsonIp.asText().equals(router.peerRouter().ipAddress().toString())) {
                    description.appendText("Peer router IP was " + jsonIp);
                    return false;
                }
            }

            JsonNode jsonProuterMac = jsonPeerRouter.get(MAC_ADDRESS);

            if (jsonProuterMac != null) {
                if (!jsonProuterMac.asText().equals(router.peerRouter().macAddress().toString())) {
                    description.appendText("Peer router MAC was " + jsonMac);
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(router.toString());
    }

    /**
     * Factory to allocate a kubevirt router matcher.
     *
     * @param router kubevirt router object we are looking for
     * @return matcher
     */
    public static KubevirtRouterJsonMatcher matchesKubevirtRouter(KubevirtRouter router) {
        return new KubevirtRouterJsonMatcher(router);
    }
}
