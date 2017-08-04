/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.onlab.packet.Ethernet;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher for ethernet objects.
 */
public final class EthernetJsonMatcher extends TypeSafeMatcher<JsonNode> {

    private final Ethernet ethernet;
    private String reason = "";

    private EthernetJsonMatcher(Ethernet ethernetValue) {
        ethernet = ethernetValue;
    }

    @Override
    public boolean matchesSafely(JsonNode jsonEthernet) {

        // check source MAC
        final JsonNode jsonSourceMacNode = jsonEthernet.get("srcMac");
        if (ethernet.getSourceMAC() != null) {
            final String jsonSourceMac = jsonSourceMacNode.textValue();
            final String sourceMac = ethernet.getSourceMAC().toString();
            if (!jsonSourceMac.equals(sourceMac)) {
                reason = "source MAC " + ethernet.getSourceMAC().toString();
                return false;
            }
        } else {
            //  source MAC not specified, JSON representation must be empty
            if (jsonSourceMacNode != null) {
                reason = "source mac should be null ";
                return false;
            }
        }

        // check destination MAC
        final JsonNode jsonDestinationMacNode = jsonEthernet.get("destMac");
        if (ethernet.getDestinationMAC() != null) {
            final String jsonDestinationMac = jsonDestinationMacNode.textValue();
            final String destinationMac = ethernet.getDestinationMAC().toString();
            if (!jsonDestinationMac.equals(destinationMac)) {
                reason = "destination MAC " + ethernet.getDestinationMAC().toString();
                return false;
            }
        } else {
            //  destination MAC not specified, JSON representation must be empty
            if (jsonDestinationMacNode != null) {
                reason = "destination mac should be null ";
                return false;
            }
        }

        // check priority code
        final short jsonPriorityCode = jsonEthernet.get("priorityCode").shortValue();
        final short priorityCode = ethernet.getPriorityCode();
        if (jsonPriorityCode != priorityCode) {
            reason = "priority code " + Short.toString(ethernet.getPriorityCode());
            return false;
        }

        // check vlanId
        final short jsonVlanId = jsonEthernet.get("vlanId").shortValue();
        final short vlanId = ethernet.getVlanID();
        if (jsonVlanId != vlanId) {
            reason = "vlan id " + Short.toString(ethernet.getVlanID());
            return false;
        }

        // check etherType
        final short jsonEtherType = jsonEthernet.get("etherType").shortValue();
        final short etherType = ethernet.getEtherType();
        if (jsonEtherType != etherType) {
            reason = "etherType " + Short.toString(ethernet.getEtherType());
            return false;
        }

        // check pad
        final boolean jsonPad = jsonEthernet.get("pad").asBoolean();
        final boolean pad = ethernet.isPad();
        if (jsonPad != pad) {
            reason = "pad " + Boolean.toString(ethernet.isPad());
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(reason);
    }

    /**
     * Factory to allocate a ethernet matcher.
     *
     * @param ethernet ethernet object we are looking for
     * @return matcher
     */
    public static EthernetJsonMatcher matchesEthernet(Ethernet ethernet) {
        return new EthernetJsonMatcher(ethernet);
    }
}
