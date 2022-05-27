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
import org.onlab.packet.IpAddress;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Hamcrest matcher for kubevirt port.
 */
public final class KubevirtPortJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtPort port;

    private static final String VM_NAME = "vmName";
    private static final String NETWORK_ID = "networkId";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";

    private KubevirtPortJsonMatcher(KubevirtPort port) {
        this.port = port;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check VM name
        String jsonVmName = jsonNode.get(VM_NAME).asText();
        String vmName = port.vmName();
        if (!jsonVmName.equals(vmName)) {
            description.appendText("VM name was " + jsonVmName);
            return false;
        }

        // check network ID
        String jsonNetworkId = jsonNode.get(NETWORK_ID).asText();
        String networkId = port.networkId();
        if (!jsonNetworkId.equals(networkId)) {
            description.appendText("network ID was " + jsonNetworkId);
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
        JsonNode jsonIpAddress = jsonNode.get(IP_ADDRESS);
        if (jsonIpAddress != null) {
            IpAddress ipAddress = port.ipAddress();
            if (!jsonIpAddress.asText().equals(ipAddress.toString())) {
                description.appendText("IP address was " + jsonIpAddress.asText());
                return false;
            }
        }

        // check device ID
        JsonNode jsonDeviceId = jsonNode.get(DEVICE_ID);
        if (jsonDeviceId != null) {
            DeviceId deviceId = port.deviceId();
            if (!jsonDeviceId.asText().equals(deviceId.toString())) {
                description.appendText("Device ID was " + jsonDeviceId.asText());
                return false;
            }
        }

        // check port number
        JsonNode jsonPortNumber = jsonNode.get(PORT_NUMBER);
        if (jsonPortNumber != null) {
            PortNumber portNUmber = port.portNumber();
            if (!jsonPortNumber.asText().equals(portNUmber.toString())) {
                description.appendText("Port number was " + jsonPortNumber.asText());
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
     * Factory to allocate an kubevirt port matcher.
     *
     * @param port kubevirt port object we are looking for
     * @return matcher
     */
    public static KubevirtPortJsonMatcher matchesKubevirtPort(KubevirtPort port) {
        return new KubevirtPortJsonMatcher(port);
    }
}
