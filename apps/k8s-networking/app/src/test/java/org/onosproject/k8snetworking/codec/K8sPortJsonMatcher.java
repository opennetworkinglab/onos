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
import org.onosproject.k8snetworking.api.K8sPort;

/**
 * Hamcrest matcher for kubernetes port.
 */
public final class K8sPortJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final K8sPort port;

    private static final String NETWORK_ID = "networkId";
    private static final String PORT_ID = "portId";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String STATE = "state";

    private K8sPortJsonMatcher(K8sPort port) {
        this.port = port;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check network ID
        String jsonNetworkId = jsonNode.get(NETWORK_ID).asText();
        String networkId = port.networkId();
        if (!jsonNetworkId.equals(networkId)) {
            description.appendText("network ID was " + jsonNetworkId);
            return false;
        }

        // check port ID
        String jsonPortId = jsonNode.get(PORT_ID).asText();
        String portId = port.portId();
        if (!jsonPortId.equals(portId)) {
            description.appendText("port ID was " + jsonPortId);
            return false;
        }

        // check MAC address
        String jsonMacAddress = jsonNode.get(MAC_ADDRESS).asText();
        String macAddress = port.macAddress().toString();
        if (!jsonMacAddress.equals(macAddress)) {
            description.appendText("MAC address was " + jsonMacAddress);
            return false;
        }

        // check IP address
        String jsonIpAddress = jsonNode.get(IP_ADDRESS).asText();
        String ipAddress = port.ipAddress().toString();
        if (!jsonIpAddress.equals(ipAddress)) {
            description.appendText("IP address was " + jsonIpAddress);
            return false;
        }

        // check device ID
        String jsonDeviceId = jsonNode.get(DEVICE_ID).asText();
        String deviceId = port.deviceId().toString();
        if (jsonDeviceId != null) {
            if (!jsonDeviceId.equals(deviceId)) {
                description.appendText("device ID was " + jsonDeviceId);
                return false;
            }
        }

        // check port number
        String jsonPortNumber = jsonNode.get(PORT_NUMBER).asText();
        String portNumber = port.portNumber().toString();
        if (jsonPortNumber != null) {
            if (!jsonPortNumber.equals(portNumber)) {
                description.appendText("port number was " + jsonPortNumber);
                return false;
            }
        }

        // check state
        String jsonState = jsonNode.get(STATE).asText();
        String state = port.state().name();
        if (jsonState != null) {
            if (!jsonState.equals(state)) {
                description.appendText("state was " + jsonState);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(port.toString());
    }

    /**
     * Factory to allocate an kubernetes port matcher.
     *
     * @param port kubernetes port object we are looking for
     * @return matcher
     */
    public static K8sPortJsonMatcher matchesK8sPort(K8sPort port) {
        return new K8sPortJsonMatcher(port);
    }
}
