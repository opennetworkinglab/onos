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
import org.onosproject.k8snetworking.api.K8sNetwork;

/**
 * Hamcrest matcher for kubernetes network.
 */
public final class K8sNetworkJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final K8sNetwork network;

    private static final String NETWORK_ID = "networkId";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MTU = "mtu";
    private static final String SEGMENT_ID = "segmentId";
    private static final String CIDR = "cidr";

    private K8sNetworkJsonMatcher(K8sNetwork network) {
        this.network = network;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check network ID
        String jsonNetworkId = jsonNode.get(NETWORK_ID).asText();
        String networkId = network.networkId();
        if (!jsonNetworkId.equals(networkId)) {
            description.appendText("network ID was " + jsonNetworkId);
            return false;
        }

        // check network name
        String jsonName = jsonNode.get(NAME).asText();
        String name = network.name();
        if (!jsonName.equals(name)) {
            description.appendText("name was " + jsonName);
            return false;
        }

        // check type
        JsonNode jsonType = jsonNode.get(TYPE);
        if (jsonType != null) {
            String type = network.type().name();
            if (!jsonType.asText().equals(type)) {
                description.appendText("network type was " + jsonType);
                return false;
            }
        }

        // check MTU
        int jsonMtu = jsonNode.get(MTU).asInt();
        int mtu = network.mtu();
        if (jsonMtu != mtu) {
            description.appendText("MTU was " + jsonMtu);
            return false;
        }

        // check segment ID
        JsonNode jsonSegmentId = jsonNode.get(SEGMENT_ID);
        if (jsonSegmentId != null) {
            String segmentId = network.segmentId();
            if (!jsonSegmentId.asText().equals(segmentId)) {
                description.appendText("segment ID was " + jsonSegmentId);
                return false;
            }
        }

        // check CIDR
        String jsonCidr = jsonNode.get(CIDR).asText();
        String cidr = network.cidr();
        if (!jsonCidr.equals(cidr)) {
            description.appendText("CIDR was " + jsonCidr);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(network.toString());
    }

    /**
     * Factory to allocate a kubernetes network matcher.
     *
     * @param network kubernetes network object we are looking for
     * @return matcher
     */
    public static K8sNetworkJsonMatcher matchesK8sNetwork(K8sNetwork network) {
        return new K8sNetworkJsonMatcher(network);
    }
}
