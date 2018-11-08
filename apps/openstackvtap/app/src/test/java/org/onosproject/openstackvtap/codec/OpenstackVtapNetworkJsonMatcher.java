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
package org.onosproject.openstackvtap.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;

import java.util.Objects;

/**
 * Hamcrest matcher for openstack vtap network.
 */
public final class OpenstackVtapNetworkJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final OpenstackVtapNetwork vtapNetwork;

    private static final String MODE = "mode";
    private static final String NETWORK_ID = "networkId";
    private static final String SERVER_IP = "serverIp";

    private OpenstackVtapNetworkJsonMatcher(OpenstackVtapNetwork vtapNetwork) {
        this.vtapNetwork = vtapNetwork;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check mode
        String jsonMode = jsonNode.get(MODE).asText();
        String mode = String.valueOf(vtapNetwork.mode());
        if (!Objects.equals(jsonMode, mode)) {
            description.appendText("mode was " + jsonMode);
            return false;
        }

        // check network id
        int jsonNetworkId = jsonNode.get(NETWORK_ID).asInt();
        int networkId = vtapNetwork.networkId();
        if (jsonNetworkId != networkId) {
            description.appendText("network id was " + jsonNetworkId);
            return false;
        }

        // check server IP
        String jsonServerIp = jsonNode.get(SERVER_IP).asText();
        String serverIp = vtapNetwork.serverIp().toString();
        if (!jsonServerIp.equals(serverIp)) {
            description.appendText("Server IP was " + jsonServerIp);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(vtapNetwork.toString());
    }

    /**
     * Factory to allocate an openstack vtap network matcher.
     *
     * @param vtapNetwork openstack vtap network object we are looking for
     * @return matcher
     */
    public static OpenstackVtapNetworkJsonMatcher matchesVtapNetwork(OpenstackVtapNetwork vtapNetwork) {
        return new OpenstackVtapNetworkJsonMatcher(vtapNetwork);
    }
}
