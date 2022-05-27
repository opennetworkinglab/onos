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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;

/**
 * Hamcrest matcher for kubevirt router interface.
 */
public final class KubevirtFloatingIpJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtFloatingIp floatingIp;
    private static final String ID = "id";
    private static final String ROUTER_NAME = "routerName";
    private static final String NETWORK_NAME = "networkName";
    private static final String POD_NAME = "podName";
    private static final String VM_NAME = "vmName";
    private static final String FLOATING_IP = "floatingIp";
    private static final String FIXED_IP = "fixedIp";

    private KubevirtFloatingIpJsonMatcher(KubevirtFloatingIp floatingIp) {
        this.floatingIp = floatingIp;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check ID
        String jsonId = jsonNode.get(ID).asText();
        String id = floatingIp.id();
        if (!jsonId.equals(id)) {
            description.appendText("ID was " + jsonId);
            return false;
        }

        // check router name
        String jsonRouterName = jsonNode.get(ROUTER_NAME).asText();
        String routerName = floatingIp.routerName();
        if (!jsonRouterName.equals(routerName)) {
            description.appendText("Router name was " + jsonRouterName);
            return false;
        }

        // check network name
        String jsonNetworkName = jsonNode.get(NETWORK_NAME).asText();
        String networkName = floatingIp.networkName();
        if (!jsonNetworkName.equals(networkName)) {
            description.appendText("Network name was " + jsonNetworkName);
            return false;
        }

        // check floating IP
        String jsonFip = jsonNode.get(FLOATING_IP).asText();
        String fip = floatingIp.floatingIp().toString();
        if (!jsonFip.equals(fip)) {
            description.appendText("Floating IP was " + jsonFip);
            return false;
        }

        // check POD name
        JsonNode jsonPodName = jsonNode.get(POD_NAME);
        if (jsonPodName != null) {
            if (!floatingIp.podName().equals(jsonPodName.asText())) {
                description.appendText("POD name was " + jsonPodName);
                return false;
            }
        }

        JsonNode jsonVmName = jsonNode.get(VM_NAME);
        if (jsonVmName != null) {
            if (!floatingIp.vmName().equals(jsonVmName.asText())) {
                description.appendText("VM name was " + jsonVmName);
                return false;
            }
        }


        // check fixed IP
        JsonNode jsonFixedIp = jsonNode.get(FIXED_IP);
        if (jsonFixedIp != null) {
            if (!floatingIp.fixedIp().toString().equals(jsonFixedIp.asText())) {
                description.appendText("Fixed IP was " + jsonFixedIp);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(floatingIp.toString());
    }

    /**
     * Factory to allocate a kubevirt floating IP matcher.
     *
     * @param fip kubevirt floating IP object we are looking for
     * @return matcher
     */
    public static KubevirtFloatingIpJsonMatcher matchesKubevirtFloatingIp(KubevirtFloatingIp fip) {
        return new KubevirtFloatingIpJsonMatcher(fip);
    }
}
