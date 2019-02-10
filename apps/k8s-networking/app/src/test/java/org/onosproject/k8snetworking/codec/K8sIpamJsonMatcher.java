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
package org.onosproject.k8snetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.k8snetworking.api.K8sIpam;

/**
 * Hamcrest matcher for kubernetes IPAM.
 */
public final class K8sIpamJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final K8sIpam ipam;

    private static final String IPAM_ID = "ipamId";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String NETWORK_ID = "networkId";

    private K8sIpamJsonMatcher(K8sIpam ipam) {
        this.ipam = ipam;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check IPAM ID
        String jsonIpamId = jsonNode.get(IPAM_ID).asText();
        String ipamId = ipam.ipamId();
        if (!jsonIpamId.equals(ipamId)) {
            description.appendText("IPAM ID was " + jsonIpamId);
            return false;
        }

        // check IP address
        String jsonIpAddress = jsonNode.get(IP_ADDRESS).asText();
        String ipAddress = ipam.ipAddress().toString();
        if (!jsonIpAddress.equals(ipAddress)) {
            description.appendText("IP address was " + jsonIpAddress);
            return false;
        }

        // check network ID
        String jsonNetworkId = jsonNode.get(NETWORK_ID).asText();
        String networkId = ipam.networkId();
        if (!jsonNetworkId.equals(networkId)) {
            description.appendText("Network ID was " + jsonNetworkId);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(ipam.toString());
    }

    /**
     * Factory to allocate kubernetes IPAM matcher.
     *
     * @param ipam kubernetes IPAM object we are looking for
     * @return matcher
     */
    public static K8sIpamJsonMatcher matchesK8sIpam(K8sIpam ipam) {
        return new K8sIpamJsonMatcher(ipam);
    }
}
